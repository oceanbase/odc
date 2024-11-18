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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.oceanbase.odc.service.monitor.MonitorAutoConfiguration.ApplicationMeterRegistry;

import io.micrometer.prometheus.PrometheusMeterRegistry;

/**
 * configuration bean in management tomcat
 */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class ManagementConfiguration {

    @Bean
    public CustomEndpoint customEndpoint(
            @Qualifier(value = "businessMeterRegistry") PrometheusMeterRegistry businessMeterRegistry) {
        return new CustomEndpoint(businessMeterRegistry);
    }

    @Bean
    public ApplicationEndpoint applicationEndpoint(ApplicationMeterRegistry applicationMeterRegistry) {
        return new ApplicationEndpoint(applicationMeterRegistry);
    }

    @Endpoint(id = "business")
    public static class CustomEndpoint {

        final PrometheusMeterRegistry meterRegistry;

        public CustomEndpoint(PrometheusMeterRegistry businessMeterRegistry) {
            this.meterRegistry = businessMeterRegistry;
        }

        @ReadOperation
        public String customEndpointMethod() {
            return meterRegistry.scrape();
        }
    }

    @Endpoint(id = "application")
    public static class ApplicationEndpoint {

        final PrometheusMeterRegistry businessMeterRegistry;

        public ApplicationEndpoint(PrometheusMeterRegistry businessMeterRegistry) {
            this.businessMeterRegistry = businessMeterRegistry;
        }

        @ReadOperation
        public String customEndpointMethod() {
            return businessMeterRegistry.scrape();
        }
    }
}
