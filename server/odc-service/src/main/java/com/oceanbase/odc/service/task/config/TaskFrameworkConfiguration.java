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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.common.ConditionOnServer;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.task.caller.DefaultK8sJobClientSelector;
import com.oceanbase.odc.service.task.caller.K8sJobClientSelector;
import com.oceanbase.odc.service.task.caller.NativeK8sJobClient;
import com.oceanbase.odc.service.task.caller.NullK8sJobClientSelector;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.jasypt.DefaultJasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.jasypt.JasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.schedule.DefaultJobCredentialProvider;
import com.oceanbase.odc.service.task.schedule.JobCredentialProvider;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.MonitorProcessRateLimiter;
import com.oceanbase.odc.service.task.schedule.StartJobRateLimiter;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.ulisesbocchio.jasyptspringboot.properties.JasyptEncryptorConfigurationProperties;
import com.ulisesbocchio.jasyptspringboot.util.Singleton;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
@Configuration
@Slf4j
public class TaskFrameworkConfiguration {

    @Lazy
    @Bean
    @ConditionalOnMissingBean(JobCredentialProvider.class)
    public JobCredentialProvider jobCredentialProvider(CloudEnvConfigurations cloudEnvConfigurations) {
        return new DefaultJobCredentialProvider(cloudEnvConfigurations);
    }

    @Lazy
    @Bean
    @ConditionalOnMissingBean(K8sJobClientSelector.class)
    public K8sJobClientSelector k8sJobClientSelector(@Autowired TaskFrameworkProperties taskFrameworkProperties)
            throws IOException {
        K8sProperties k8sProperties = taskFrameworkProperties.getK8sProperties();
        if (StringUtils.isBlank(k8sProperties.getKubeUrl())) {
            log.info("local task k8s cluster is not enabled.");
            return new NullK8sJobClientSelector();
        }
        log.info("build k8sJobClientSelector, kubeUrl={}, namespace={}",
                k8sProperties.getKubeUrl(), k8sProperties.getNamespace());
        NativeK8sJobClient nativeK8sJobClient = new NativeK8sJobClient(k8sProperties);
        return new DefaultK8sJobClientSelector(nativeK8sJobClient);
    }

    @Bean
    @ConditionalOnBean(TaskFrameworkService.class)
    public StartJobRateLimiter monitorProcessRateLimiter(@Autowired TaskFrameworkService taskFrameworkService) {
        return new MonitorProcessRateLimiter(TaskFrameworkPropertiesSupplier.getSupplier(), taskFrameworkService);
    }

    @Bean
    public JasyptEncryptorConfigProperties JasyptEncryptorConfigProperties(
            @Autowired Singleton<JasyptEncryptorConfigurationProperties> configPropertiesSingleton) {
        return new DefaultJasyptEncryptorConfigProperties(configPropertiesSingleton);
    }

    @Lazy
    @Bean("taskFrameworkSchedulerFactoryBean")
    @ConditionalOnBean(TaskFrameworkProperties.class)
    @ConditionOnServer
    public SchedulerFactoryBean taskFrameworkSchedulerFactoryBean(
            TaskFrameworkProperties taskFrameworkProperties,
            @Qualifier("taskFrameworkMonitorExecutor") ThreadPoolTaskExecutor executor) {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        String taskFrameworkSchedulerName = "TASK-FRAMEWORK-SCHEDULER";
        schedulerFactoryBean.setSchedulerName(taskFrameworkSchedulerName);
        schedulerFactoryBean.setStartupDelay(taskFrameworkProperties.getQuartzStartDelaySeconds());
        schedulerFactoryBean.setTaskExecutor(executor);
        return schedulerFactoryBean;
    }

    @Bean
    @ConditionalOnBean(TaskFrameworkProperties.class)
    public TaskFrameworkEnabledProperties taskFrameworkEnabledProperties(
            @Autowired TaskFrameworkProperties taskFrameworkProperties) {
        TaskFrameworkEnabledProperties properties = new TaskFrameworkEnabledProperties();
        boolean enabled = taskFrameworkProperties.isEnabled();
        properties.setEnabled(enabled);
        log.info("Task-framework isEnabled={}.", properties.isEnabled());
        return properties;
    }

    @Bean
    @ConditionOnServer
    public JobConfiguration jobConfiguration() {
        return new DefaultSpringJobConfiguration();
    }

    @Bean
    @ConditionalOnBean(JobConfiguration.class)
    public JobSchedulerFactoryBean jobSchedulerFactoryBean(@Autowired JobConfiguration jobConfiguration) {
        JobSchedulerFactoryBean factoryBean = new JobSchedulerFactoryBean();
        factoryBean.setJobConfiguration(jobConfiguration);
        return factoryBean;
    }

    @Bean
    @ConditionalOnMissingBean
    public JobScheduler jobScheduler() {
        return new JobScheduler() {
            @Override
            public Long scheduleJobNow(JobDefinition jd) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void cancelJob(Long jobId) throws JobException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void modifyJobParameters(Long jobId, Map<String, String> jobParameters) throws JobException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void await(Long jobId, Integer timeout, TimeUnit timeUnit) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public EventPublisher getEventPublisher() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
