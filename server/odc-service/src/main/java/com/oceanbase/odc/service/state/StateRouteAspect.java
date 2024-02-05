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

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

import java.lang.reflect.Method;

import javax.validation.constraints.NotNull;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;

import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@Slf4j
public class StateRouteAspect {

    @Autowired
    private HostProperties properties;

    @Autowired
    private RouteHealthManager routeHealthManager;
    @Autowired
    private RequestDispatcher requestDispatcher;

    @Pointcut("@annotation(com.oceanbase.odc.service.state.StatefulRoute)")
    public void stateRouteMethods() {}

    @Around("stateRouteMethods()")
    public Object aroundStateRouteExecution(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        StatefulRoute statefulRoute = findAnnotation(method, StatefulRoute.class);
        boolean nodeChanged = false;
        StateManager stateManager = null;
        RouteInfo routeInfo = null;
        if (statefulRoute != null) {
            Object stateIdBySePL = parseStateIdFromParameter(proceedingJoinPoint, statefulRoute.stateIdExpression());
            stateManager = getStateManager(statefulRoute);
            routeInfo = stateManager.getRouteInfo(stateIdBySePL);
            if (routeHealthManager.isHealthy(routeInfo)) {
                DispatchResponse dispatchResponse =
                        requestDispatcher.forward(routeInfo.getHostName(), routeInfo.getPort());
                logTrace(method, stateIdBySePL, routeInfo);
                StateRouteFilter.getContext().setDispatchResponse(dispatchResponse);
                return null;
            } else {
                // means node changed
                if (!routeInfo.isCurrentNode(properties)) {
                    nodeChanged = true;
                    stateManager.preHandleBeforeNodeChange(proceedingJoinPoint, routeInfo);
                }
            }
        }
        Object proceed = proceedingJoinPoint.proceed();
        if (nodeChanged) {
            Preconditions.checkNotNull(stateManager);
            stateManager.afterHandleBeforeNodeChange(proceedingJoinPoint, routeInfo);
        }
        return proceed;
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
        if (statefulRoute.stateManager() != null) {
            Object bean = SpringContextUtil.getBean(statefulRoute.stateManager());
            Preconditions.checkArgument(bean instanceof StateManager, "illegal stateManager type");
            stateManager = (StateManager) bean;
        }
        Preconditions.checkNotNull(stateManager, "stateManager");
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
