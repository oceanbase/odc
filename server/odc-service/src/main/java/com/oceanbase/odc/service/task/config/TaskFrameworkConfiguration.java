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
import java.util.Optional;
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
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.common.ConditionOnServer;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.resource.K8sResourceManager;
import com.oceanbase.odc.service.resource.OcpK8sResourceManager;
import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceMetaStore;
import com.oceanbase.odc.service.resource.ResourceMode;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.K8SResourceOperator;
import com.oceanbase.odc.service.resource.k8s.K8sResource;
import com.oceanbase.odc.service.resource.k8s.client.DefaultK8sJobClientSelector;
import com.oceanbase.odc.service.resource.k8s.client.K8sJobClientSelector;
import com.oceanbase.odc.service.resource.k8s.client.NativeK8sJobClient;
import com.oceanbase.odc.service.resource.k8s.client.NullK8sJobClientSelector;
import com.oceanbase.odc.service.resource.local.LocalResourceOperator;
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
    @ConditionalOnMissingBean(K8sResourceManager.class)
    public K8sResourceManager k8SResourceManager(@Autowired TaskFrameworkProperties taskFrameworkProperties,
            @Autowired ResourceRepository resourceRepository)
            throws IOException {
        OcpK8sResourceManager k8sResourceManager = new OcpK8sResourceManager(createMetaStore(resourceRepository));
        registerODCCloudEndpointK8sResourceManager(taskFrameworkProperties, k8sResourceManager);
        // register other resource operator here
        return k8sResourceManager;
    }

    /**
     * operate k8s resource by odc
     */
    private void registerODCCloudEndpointK8sResourceManager(TaskFrameworkProperties taskFrameworkProperties,
            OcpK8sResourceManager k8sResourceManager) throws IOException {
        K8sProperties k8sProperties = taskFrameworkProperties.getK8sProperties();
        K8sJobClientSelector k8sJobClientSelector;
        if (StringUtils.isBlank(k8sProperties.getKubeUrl())) {
            log.info("local task k8s cluster is not enabled.");
            k8sJobClientSelector = new NullK8sJobClientSelector();
        } else {
            log.info("build k8sJobClientSelector, kubeUrl={}, namespace={}",
                    k8sProperties.getKubeUrl(), k8sProperties.getNamespace());
            NativeK8sJobClient nativeK8sJobClient = new NativeK8sJobClient(k8sProperties);
            k8sJobClientSelector = new DefaultK8sJobClientSelector(nativeK8sJobClient);
        }
        K8SResourceOperator k8SResourceOperator =
                new K8SResourceOperator(k8sJobClientSelector, k8sProperties.getPodPendingTimeoutSeconds());
        // all region use one k8s operator
        k8sResourceManager.registerK8sOperator("default", k8SResourceOperator);
    }

    /**
     * create default meta store
     * 
     * @param resourceRepository
     * @return
     */
    private ResourceMetaStore createMetaStore(ResourceRepository resourceRepository) {
        return new ResourceMetaStore() {
            @Override
            public Resource findResource(ResourceID resourceID) {
                Optional<ResourceEntity> optionalResourceEntity = resourceRepository.findByResourceID(resourceID);
                if (optionalResourceEntity.isPresent()) {
                    ResourceEntity entity = optionalResourceEntity.get();
                    return new K8sResource(resourceID.getRegion(), entity.getGroupName(),
                            entity.getNamespace(), entity.getResourceName(),
                            entity.getStatus(),
                            entity.getEndpoint(), entity.getCreateTime());
                } else {
                    log.warn("find by resourceID = {} failed", resourceID);
                    return null;
                }
            }

            @Override
            public void saveResource(Resource resource) throws Exception {
                ResourceEntity resourceEntity = new ResourceEntity();
                resourceEntity.setResourceMode(ResourceMode.REMOTE_K8S);
                resourceEntity.setEndpoint(resource.endpoint().getResourceURL());
                resourceEntity.setCreateTime(resource.createDate());
                resourceEntity.setGroupName(resourceEntity.getGroupName());
                resourceEntity.setResourceName(resourceEntity.getResourceName());
                resourceEntity.setStatus(resourceEntity.getStatus());
                resourceRepository.save(resourceEntity);
            }

            @Override
            public int updateResourceState(ResourceID resourceID, ResourceState resourceState) {
                int updateRow = resourceRepository.updateResourceStatus(resourceID, resourceState.name());
                if (0 == updateRow) {
                    log.info("update resource = {}, affect 0 rows", resourceID);
                }
                return updateRow;
            }

            @Override
            public int deleteResource(ResourceID resourceID) {
                int affectRow = resourceRepository.deleteResource(resourceID);
                if (0 == affectRow) {
                    log.info("delete resource = {}, affect 0 rows", resourceID);
                }
                return affectRow;
            }
        };
    }

    @Lazy
    @Bean
    @ConditionalOnMissingBean(LocalResourceOperator.class)
    public LocalResourceOperator memoryResourceManager(@Autowired TaskFrameworkProperties taskFrameworkProperties)
            throws IOException {
        // TODO(lx): impl meta store
        return new LocalResourceOperator();
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
