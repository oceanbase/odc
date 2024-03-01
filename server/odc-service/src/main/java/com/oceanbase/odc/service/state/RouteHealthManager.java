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

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.service.state.model.RouteInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ConditionalOnProperty(value = {"odc.web.stateful-route.enabled"}, havingValue = "true")
public class RouteHealthManager implements InitializingBean {

    private static final Map<RouteInfo, RouteManageInfo> ROUTE_HEALTHY_MAP = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Value("${odc.web.stateful-route.host.expire_seconds:3600}")
    private Integer expireSeconds;

    public boolean isHealthy(RouteInfo routeInfo) {
        RouteManageInfo routeManageInfo = ROUTE_HEALTHY_MAP.computeIfAbsent(routeInfo,
                r -> new RouteManageInfo(r.isHealthyHost(), LocalDateTime.now()));
        routeManageInfo.now();
        return routeManageInfo.isHealthy();
    }

    @Override
    public void afterPropertiesSet() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                doTest();
            } catch (Exception e) {
                log.info("test host error", e);
            }
        }, 1, 10, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void preDestroy() {
        ExecutorUtils.gracefulShutdown(scheduledExecutorService, "stateManager", 5);
    }


    private void doTest() {
        for (Iterator<Entry<RouteInfo, RouteManageInfo>> it = ROUTE_HEALTHY_MAP.entrySet().iterator(); it.hasNext();) {
            Map.Entry<RouteInfo, RouteManageInfo> item = it.next();
            RouteManageInfo manageInfo = item.getValue();
            if (manageInfo.expired(expireSeconds)) {
                it.remove();
            }
            manageInfo.setHealthy(item.getKey().isHealthyHost());
        }
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class RouteManageInfo {
        boolean healthy;
        LocalDateTime lastCheckTime;

        public void now() {
            lastCheckTime = LocalDateTime.now();
        }

        public boolean expired(Integer expireSeconds) {
            return LocalDateTime.now().isAfter(lastCheckTime.plusSeconds(expireSeconds));
        }
    }
}
