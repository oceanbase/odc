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
package com.oceanbase.odc.service.monitor;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.monitor.task.TaskMetrics;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class MonitorConfiguration {

    @Bean
    TaskMetrics taskMetrics(MeterRegistry meterRegistry, MonitorProperties monitorProperties) {
        return new TaskMetrics(meterRegistry, monitorProperties);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("ServerHost", SystemUtils.getHostName());
    }
}
