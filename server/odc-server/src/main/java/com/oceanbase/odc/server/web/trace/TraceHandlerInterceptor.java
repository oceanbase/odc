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
package com.oceanbase.odc.server.web.trace;

import static com.oceanbase.odc.core.alarm.AlarmEventNames.API_TOO_LONG_RT_TIME;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * for trace info initialize and destroy for each request
 */
@Slf4j
@Component
public class TraceHandlerInterceptor implements HandlerInterceptor {

    private static final String SERVER = SystemUtils.getHostName();
    private static final String ALARM_RT_TIME_KEY = "odc.monitor.alarm.rt.time";
    private static final Long DEFAULT_ALARM_RT_MILLISECONDS = 20 * 1000L;

    @Autowired
    private WebTraceUtils webTraceUtils;

    @Autowired
    private SystemConfigService systemConfigService;

    private final Cache<String, Long> configCache =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(2, TimeUnit.MINUTES).build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TraceContextHolder.trace();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
        try {
            String userId = webTraceUtils.isAuthRequired() ? webTraceUtils.currentUserId() : "Not-Required";
            String clientAddress = webTraceUtils.clientAddress();
            String userAgent = webTraceUtils.userAgent();
            String fullURL = webTraceUtils.fullURL();
            String methodName = getReturnType(handler);
            long rt = TraceContextHolder.getDuration();
            String httpMethod = request.getMethod();
            int httpStatus = response.getStatus();
            String apiMsg = String.format(
                    "userId=%s, serverName=%s, httpMethod=%s, httpStatus=%s, odcCode=%s, fullUrl=%s, methodName=%s, rt=%s, userAgent=%s, clientAddress=%s",
                    userId, SERVER, httpMethod,
                    httpStatus, TraceContextHolder.getOdcCode(), fullURL,
                    methodName, rt, userAgent, clientAddress);
            log.info(apiMsg);
            if (rt > getAlarmRtMillSecond()) {
                String requestId = TraceContextHolder.getRequestId();
                String alarmMsg = apiMsg + "requestId=" + requestId;
                AlarmUtils.alarm(API_TOO_LONG_RT_TIME, alarmMsg);
            }
        } finally {
            TraceContextHolder.clear();
        }
    }

    private String getReturnType(Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            return method.getName();
        }
        return "";
    }

    private Long getAlarmRtMillSecond() {
        return configCache.get(ALARM_RT_TIME_KEY, (key) -> {
            List<Configuration> configurations = systemConfigService.queryByKeyPrefix(ALARM_RT_TIME_KEY);
            if (CollectionUtils.isNotEmpty(configurations)) {
                return Long.valueOf(configurations.get(0).getValue());
            }
            return DEFAULT_ALARM_RT_MILLISECONDS;
        });
    }
}
