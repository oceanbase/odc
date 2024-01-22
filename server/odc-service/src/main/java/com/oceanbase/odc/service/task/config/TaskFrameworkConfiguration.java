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

package com.oceanbase.odc.service.task.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.oceanbase.odc.service.common.util.ConditionalOnProperty;
import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.caller.NativeK8sJobClient;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
@ConditionalOnProperty(prefix = "odc.task-framework", name = "enable-task-framework", havingValues = "true")
@Configuration
@Slf4j
public class TaskFrameworkConfiguration {

    @Lazy
    @Bean
    @ConditionalOnMissingBean(K8sJobClient.class)
    public K8sJobClient k8sJobClient(@Autowired TaskFrameworkProperties taskFrameworkProperties) {
        try {
            log.info("k8s url is {}", taskFrameworkProperties.getK8sProperties().getKubeUrl());
            log.info("k8s namespace is {}", taskFrameworkProperties.getK8sProperties().getNamespace());
            return new NativeK8sJobClient(taskFrameworkProperties.getK8sProperties());
        } catch (IOException e) {
            log.warn("Create NativeK8sJobClient occur error:", e);
            return null;
        }
    }

    @Bean
    public JobConfiguration jobConfiguration() {
        return new DefaultSpringJobConfiguration();
    }

    @Bean
    public JobSchedulerFactoryBean jobSchedulerFactoryBean(@Autowired JobConfiguration jobConfiguration) {
        JobSchedulerFactoryBean factoryBean = new JobSchedulerFactoryBean();
        factoryBean.setJobConfiguration(jobConfiguration);
        return factoryBean;
    }

}
