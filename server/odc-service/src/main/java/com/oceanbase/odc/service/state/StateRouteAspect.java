/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.state;

import static com.oceanbase.odc.service.monitor.DefaultMeterName.STATEFUL_ROUTE_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.STATEFUL_ROUTE_UNHEALTHY_COUNT;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.HttpRequestProvider;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterKey.Builder;
import com.oceanbase.odc.service.monitor.MeterManager;
import com.oceanbase.odc.service.session.factory.StateHostGenerator;
import com.oceanbase.odc.service.state.model.RouteInfo;
import com.oceanbase.odc.service.state.model.SingleNodeStateResponse;
import com.oceanbase.odc.service.state.model.StateManager;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@Slf4j
@ConditionalOnProperty(value = {"odc.web.stateful-route.enabled"}, havingValue = "true")
public class StateRouteAspect {

    @Autowired
    private HostProperties properties;

    @Autowired
    private RouteHealthManager routeHealthManager;
    @Autowired
    private RequestDispatcher requestDispatcher;

    @Autowired
    private HttpRequestProvider requestProvider;

    @Autowired
    private ThreadPoolExecutor statefulRouteThreadPoolExecutor;

    @Autowired
    private StateHostGenerator stateHostGenerator;

    @Autowired
    private MeterManager meterManager;

    @Pointcut("@annotation(com.oceanbase.odc.service.state.model.StatefulRoute)")
    public void stateRouteMethods() {}

    @Around("stateRouteMethods()")
    public Object aroundStateRouteExecution(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        StatefulRoute statefulRoute = findAnnotation(method, StatefulRoute.class);
        StateManager stateManager = null;
        RouteInfo routeInfo = null;
        if (statefulRoute != null) {
            Object stateIdBySePL = parseStateIdFromParameter(proceedingJoinPoint, statefulRoute.stateIdExpression());
            stateManager = getStateManager(statefulRoute);
            if (statefulRoute.multiState()) {
                Preconditions.checkArgument(stateManager.supportMultiRoute(), "stateManager not support multi state");
                return handleMultiState(stateManager, stateIdBySePL, proceedingJoinPoint);
            } else {
                routeInfo = stateManager.getRouteInfo(stateIdBySePL);
                Verify.notNull(routeInfo, "routeInfo");
                boolean healthyNode = routeHealthManager.isHealthy(routeInfo);
                boolean notCurrentNode =
                        !routeInfo.isCurrentNode(properties.getRequestPort(), stateHostGenerator.getHost());
                sendMetric(routeInfo);
                log.debug("sate routeInfo={}, host={},port={}", routeInfo, stateHostGenerator.getHost(),
                        properties.getRequestPort());
                log.debug("healthyNode={}, notCurrentNode={}", healthyNode, notCurrentNode);
                if (notCurrentNode && healthyNode) {
                    DispatchResponse dispatchResponse =
                            requestDispatcher.forward(routeInfo.getHostName(), routeInfo.getPort());
                    logTrace(method, stateIdBySePL, routeInfo);
                    StateRouteFilter.getContext().setDispatchResponse(dispatchResponse);
                    return null;
                }
                if (!healthyNode) {
                    sendUnhealthyMetric(routeInfo);
                    AlarmUtils.alarm(AlarmEventNames.STATEFUL_ROUTE_NOT_HEALTHY,
                            "can't arrive route info " + routeInfo);
                }
            }
        }
        return proceedingJoinPoint.proceed();
    }

    private void sendMetric(RouteInfo routeInfo) {
        MeterKey meterKey = Builder.ofMeter(STATEFUL_ROUTE_COUNT).addTag("host", routeInfo.getHostName()).build();
        meterManager.incrementCounter(meterKey);
    }

    private void sendUnhealthyMetric(RouteInfo routeInfo) {
        MeterKey meterKey =
                Builder.ofMeter(STATEFUL_ROUTE_UNHEALTHY_COUNT).addTag("host", routeInfo.getHostName()).build();
        meterManager.incrementCounter(meterKey);
    }

    private Object handleMultiState(StateManager stateManager, Object stateIdBySePL,
            ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Set<RouteInfo> allRoutes = stateManager.getAllRoutes(stateIdBySePL);
        List<SingleNodeStateResponse> allResponse = new ArrayList<>();

        RouteInfo currentNode = allRoutes.stream()
                .filter(r -> r.isCurrentNode(properties.getRequestPort(), stateHostGenerator.getHost())).findFirst()
                .orElse(null);
        Object proceed = null;
        if (currentNode != null) {
            proceed = proceedingJoinPoint.proceed();
        }
        Set<RouteInfo> otherNodeRoute = allRoutes.stream()
                .filter(r -> !r.isCurrentNode(properties.getRequestPort(), stateHostGenerator.getHost())).collect(
                        Collectors.toSet());

        if (CollectionUtils.isEmpty(otherNodeRoute)) {
            return proceed;
        }

        if (CollectionUtils.isNotEmpty(otherNodeRoute)) {
            final HttpServletRequest request = requestProvider.getRequest();
            final ByteArrayOutputStream requestBody = requestProvider.getRequestBody();
            List<Pair<RouteInfo, Future<DispatchResponse>>> routeResponse = otherNodeRoute.stream().map(r -> {
                Future<DispatchResponse> future = statefulRouteThreadPoolExecutor.submit(
                        () -> requestDispatcher.forward(r.getHostName(), r.getPort(), request, requestBody));
                return new Pair<>(r, future);
            }).collect(Collectors.toList());
            for (Pair<RouteInfo, Future<DispatchResponse>> rr : routeResponse) {
                RouteInfo ri = rr.left;
                Future<DispatchResponse> future = rr.right;
                try {
                    DispatchResponse dispatchResponse = future.get();
                    allResponse.add(new SingleNodeStateResponse(stateIdBySePL, ri, dispatchResponse));
                } catch (Exception e) {
                    SingleNodeStateResponse error = SingleNodeStateResponse.error(stateIdBySePL, ri, e);
                    allResponse.add(error);
                }
            }
        }
        return stateManager.handleMultiResponse(allResponse, proceed);
    }

    private void logTrace(Method method, Object stateId, RouteInfo dispatchTo) {
        if (log.isDebugEnabled()) {
            String currentNodeHostName = SystemUtils.getHostName();
            String currentNodeIpAddress =
                    properties.getOdcHost() == null ? SystemUtils.getLocalIpAddress() : properties.getOdcHost();
            log.debug(
                    "current requestId={},traceId={},spanId={},method={},hostName={},ipAddress={}, dispatch to host={}",
                    TraceContextHolder.getRequestId(), TraceContextHolder.getTraceId(), method.getName(), stateId,
                    currentNodeHostName, currentNodeIpAddress, dispatchTo);
        }
    }

    @NotNull
    private StateManager getStateManager(StatefulRoute statefulRoute) {
        StateManager stateManager = null;
        if (statefulRoute.stateName() != StateName.NONE) {
            StateName stateName = statefulRoute.stateName();
            stateManager = SpringContextUtil.getBean(stateName.getStateManagerClass());
        }
        if (StringUtils.isNotBlank(statefulRoute.stateManager())) {
            Object bean = SpringContextUtil.getBean(statefulRoute.stateManager());
            Verify.verify(bean instanceof StateManager, "illegal stateManager type");
            stateManager = (StateManager) bean;
        }
        Verify.notNull(stateManager, "stateManager");
        return stateManager;
    }


    private Object parseStateIdFromParameter(ProceedingJoinPoint proceedingJoinPoint, String stateIdExpression) {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] parameterValues = proceedingJoinPoint.getArgs();
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], parameterValues[i]);
        }
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(stateIdExpression);
        return exp.getValue(context);
    }
}
