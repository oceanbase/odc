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
package com.oceanbase.odc.service.task.dummy;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.resource.k8s.K8sPodResource;
import com.oceanbase.odc.service.resource.k8s.K8sResourceContext;
import com.oceanbase.odc.service.resource.k8s.PodConfig;
import com.oceanbase.odc.service.resource.k8s.client.K8sJobClient;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobCredentialProvider;

/**
 * @author longpeng.zlp
 * @date 2024/8/28 14:27
 */
public class LocalMockK8sJobClientTest {
    @Ignore
    @Test
    public void testLocalProcessStart() throws JobException {
        JobConfiguration jobConfiguration = Mockito.mock(JobConfiguration.class);
        JobCredentialProvider jobCredentialProvider = Mockito.mock(JobCredentialProvider.class);
        TaskFrameworkProperties taskFrameworkProperties = Mockito.mock(TaskFrameworkProperties.class);
        Mockito.when(taskFrameworkProperties.getJobProcessMaxMemorySizeInMB()).thenReturn(2048L);
        Mockito.when(taskFrameworkProperties.getJobProcessMinMemorySizeInMB()).thenReturn(2048L);
        HostProperties hostProperties = Mockito.mock(HostProperties.class);
        Mockito.when(jobConfiguration.getJobCredentialProvider()).thenReturn(jobCredentialProvider);
        Mockito.when(jobConfiguration.getTaskFrameworkProperties()).thenReturn(taskFrameworkProperties);
        Mockito.when(jobConfiguration.getHostProperties()).thenReturn(hostProperties);
        JobConfigurationHolder.setJobConfiguration(jobConfiguration);
        LocalMockK8sJobClient localMockK8sJobClient = new LocalMockK8sJobClient();
        K8sJobClient k8sJobClient = localMockK8sJobClient.select("any");
        K8sResourceContext k8sResourceContext =
                new K8sResourceContext(new PodConfig(), "local", "local", "image", null);
        K8sPodResource k8sResource = k8sJobClient.create(k8sResourceContext);
        Assert.assertNotNull(k8sResource);
    }
}
