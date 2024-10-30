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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.executor.TraceDecoratorThreadFactory;

@Configuration
public class StatefulRouteConfiguration {

    @Value("${odc.web.stateful-route.multi-state.core-pool-size:3}")
    private Integer coolPoolSize;

    @Value("${odc.web.stateful-route.multi-state.max-pool-size:20}")
    private Integer maxPoolSize;

    @Value("${odc.web.stateful-route.multi-state.capacity:64}")
    private Integer capacity;

    @Bean
    public ThreadPoolExecutor statefulRouteThreadPoolExecutor() {
        int core = Math.max(SystemUtils.availableProcessors() * 2, coolPoolSize);
        int max = Math.max(maxPoolSize, core);
        return new ThreadPoolExecutor(
                core,
                max,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(capacity),
                new TraceDecoratorThreadFactory(r -> new Thread(r, "stateful-route-threadPool-" + r.hashCode())),
                (r, executor) -> {
                    throw new RuntimeException("stateful route threadPool is EXHAUSTED!");
                });
    }
}
