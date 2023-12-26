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

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.caller.NativeK8sJobClient;
import com.oceanbase.odc.service.task.constants.JobEnvConstants;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;
import com.oceanbase.odc.service.task.schedule.FixedHostUrlProvider;
import com.oceanbase.odc.service.task.schedule.HostUrlProvider;
import com.oceanbase.odc.service.task.schedule.IpBasedHostUrlProvider;
import com.oceanbase.odc.service.task.schedule.ServiceNameHostUrlProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
@Slf4j
@Configuration
public class TaskFrameworkConfiguration {

    @Bean
    @ConditionalOnMissingBean(K8sJobClient.class)
    public K8sJobClient k8sJobClient(@Autowired TaskFrameworkProperties taskFrameworkProperties) {
        if (taskFrameworkProperties.getRunMode() == TaskRunModeEnum.K8S) {
            try {
                log.info("k8s url is {}", taskFrameworkProperties.getK8s().getKubeUrl());
                log.info("k8s namespace is {}", taskFrameworkProperties.getK8s().getNamespace());
                return new NativeK8sJobClient(taskFrameworkProperties.getK8s());
            } catch (IOException e) {
                log.warn("Create NativeK8sJobClient occur error", e);
            }
        }
        return null;
    }

    @Bean
    public HostUrlProvider hostUrlProvider(@Autowired TaskFrameworkProperties taskFrameworkProperties,
            @Autowired HostProperties hostProperties) {
        HostUrlProvider hostUrlProvider =
            StringUtils.isNotBlank(taskFrameworkProperties.getOdcUrl()) ? new FixedHostUrlProvider(taskFrameworkProperties)
                        : ((StringUtils.isNotBlank(SystemUtils.getEnvOrProperty(JobEnvConstants.ODC_SERVICE_HOST)) &&
                            StringUtils.isNotBlank(SystemUtils.getEnvOrProperty(JobEnvConstants.ODC_SERVER_PORT)))
                                        ? new ServiceNameHostUrlProvider()
                                        : new IpBasedHostUrlProvider(hostProperties));

        log.info("Host Url Provider is {}", hostUrlProvider.getClass().getSimpleName());
        return hostUrlProvider;
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
