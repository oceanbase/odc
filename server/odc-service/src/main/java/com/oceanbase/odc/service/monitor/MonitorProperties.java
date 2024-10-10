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

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "odc.server.monitor")
@Data
public class MonitorProperties {

    @NestedConfigurationProperty
    private ActuatorProperties actuator = new ActuatorProperties();

    @NestedConfigurationProperty
    private MeterProperties meter = new MeterProperties();

    @Data
    public static class ActuatorProperties {

        private List<String> endpointPath = Collections.singletonList("/actuator/business");

    }

    @Data
    public static class MeterProperties {
        private Integer maxTimerMeterNumber = 500;
        private Integer maxCounterMeterNumber = 100;
    }
}
