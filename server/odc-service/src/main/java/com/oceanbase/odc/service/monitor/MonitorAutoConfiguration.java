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

import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.oceanbase.odc.common.util.SystemUtils;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;
import io.micrometer.core.lang.Nullable;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exemplars.ExemplarSampler;

@AutoConfiguration(before = MetricsAutoConfiguration.class)
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class MonitorAutoConfiguration {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("ServerHost", SystemUtils.getHostName());
    }


    @Bean(value = "businessMeterRegistry")
    BusinessMeterRegistry businessMeterRegistry(PrometheusConfig prometheusConfig) {
        return new BusinessMeterRegistry(prometheusConfig);
    }

    @Bean(value = "applicationMeterRegistry")
    @Primary
    ApplicationMeterRegistry applicationMeterRegistry(PrometheusConfig prometheusConfig,
            CollectorRegistry collectorRegistry, Clock clock, ObjectProvider<ExemplarSampler> exemplarSamplerProvider) {
        return new ApplicationMeterRegistry(prometheusConfig, collectorRegistry, clock,
                exemplarSamplerProvider.getIfAvailable());
    }

    @Bean
    MeterBinderProxyBeanPostProcessor meterBinderProxyBeanPostProcessor() {
        return new MeterBinderProxyBeanPostProcessor();
    }

    public static class ApplicationMeterRegistry extends PrometheusMeterRegistry {
        public ApplicationMeterRegistry(PrometheusConfig config, CollectorRegistry registry, Clock clock,
                @Nullable ExemplarSampler exemplarSampler) {
            super(config, registry, clock, exemplarSampler);
        }
    }

    public static class BusinessMeterRegistry extends PrometheusMeterRegistry {
        public BusinessMeterRegistry(PrometheusConfig config) {
            super(config);
        }
    }

    static class MeterBinderProxyBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName)
                throws BeansException {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName)
                throws BeansException {
            if (bean instanceof MeterBinder) {
                return Proxy.newProxyInstance(
                        bean.getClass().getClassLoader(),
                        bean.getClass().getInterfaces(),
                        (proxy, method, args) -> {
                            if ("bindTo".equals(method.getName()) && args.length == 1
                                    && args[0] instanceof MeterRegistry) {
                                MeterRegistry registry = (MeterRegistry) args[0];
                                // AutoConfiguration Binder only bind to application registry
                                if (registry instanceof ApplicationMeterRegistry) {
                                    return method.invoke(bean, args);
                                }
                                return null;
                            }
                            return method.invoke(bean, args);
                        });
            }
            return bean;
        }
    }


    static class NonEnabledMeterManager implements MeterManager {

        @Override
        public boolean registerGauge(MeterKey meterKey, Supplier<Number> f) {
            return false;
        }

        @Override
        public boolean incrementCounter(MeterKey meterKey) {
            return false;
        }

        @Override
        public boolean startTimerSample(String sampleKey, MeterKey meterKey) {
            return false;
        }

        @Override
        public boolean recordTimerSample(String sampleKey, MeterKey meterKey) {
            return false;
        }

    }
}
