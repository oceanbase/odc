/*
 * Copyright (c) 2024 OceanBase.
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

import static com.oceanbase.odc.service.state.StatefulControllerAdvice.STATE_ID;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import com.oceanbase.odc.metadb.stateroute.StateRouteEntity;
import com.oceanbase.odc.metadb.stateroute.StateRouteRepository;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.state.StatefulRoute.StateIdProviderType;
import com.oceanbase.odc.service.state.StatefulRoute.StateType;

@Component
public class StatefulRouteInterceptor implements HandlerInterceptor {
    @Autowired
    StateRouteRepository stateRouteRepository;
    @Autowired
    RequestDispatcher requestDispatcher;
    @Autowired
    HostProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 获取方法上的注解
            Method method = handlerMethod.getMethod();
            StatefulRoute statefulRoute = findStatefulRoute(method);
            if (statefulRoute == null) {
                return true;
            }
            StateType type = statefulRoute.type();
            if (type == StateType.ROUTING || type == StateType.DESTROYED) {
                String stateIdFromRequest = getStateIdFromRequest(statefulRoute, request);
                if (stateIdFromRequest != null) {
                    StateRouteEntity stateRoute =
                            stateRouteRepository.findByStateNameAndStateId(statefulRoute.stateName(),
                                    stateIdFromRequest);
                    RouteInfo routeInfo = stateRoute.getRouteInfo();
                    DispatchResponse dispatchResponse =
                            requestDispatcher.forward(routeInfo.getHost(), routeInfo.getPort());
                    // 复制所有响应头
                    dispatchResponse.getResponseHeaders().forEach((headerName, headerValues) -> {
                        headerValues.forEach(value -> response.addHeader(headerName, value));
                    });
                    // 设置响应状态码
                    // response.setStatus(dispatchResponse.getStatusCodeValue());
                    // 发送响应体内容
                    ServletOutputStream outputStream = response.getOutputStream();
                    outputStream.write(dispatchResponse.getContent());
                    outputStream.flush();
                    outputStream.close();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            @Nullable ModelAndView modelAndView) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 获取方法上的注解
            Method method = handlerMethod.getMethod();
            StatefulRoute statefulRoute = findStatefulRoute(method);
            if (statefulRoute == null) {
                return;
            }
            StateType type = statefulRoute.type();
            if (type == StateType.CREATED) {
                String stateId = (String) request.getAttribute(STATE_ID);
                String stateName = statefulRoute.stateName();
                StateRouteEntity entity = new StateRouteEntity(stateName, stateId, new RouteInfo(properties));
                stateRouteRepository.save(entity);
            }
        }
    }

    private StatefulRoute findStatefulRoute(Method method) {
        Annotation[] methodAnnotations = method.getDeclaredAnnotations();
        for (Annotation annotation : methodAnnotations) {
            if (annotation instanceof StatefulRoute) {
                return (StatefulRoute) annotation;
            }
        }
        return null;
    }


    public String getStateIdFromRequest(StatefulRoute statefulRoute, HttpServletRequest request) {
        StateIdProviderType type = statefulRoute.idProvider();
        String field = statefulRoute.field();
        if (type == StateIdProviderType.QUERY_PARAMETER) {
            return request.getParameter(field);
        }
        if (type == StateIdProviderType.PATH_VARIABLE) {
            Map<String, String> pathVariables =
                    (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (pathVariables != null) {
                return pathVariables.get(field);
            }
            return null;
        }
        throw new IllegalArgumentException("unmatched state id provider type");
    }
}
