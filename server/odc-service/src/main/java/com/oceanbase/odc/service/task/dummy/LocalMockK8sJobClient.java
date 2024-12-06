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

import java.util.Date;
import java.util.Optional;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.task.caller.DefaultExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobCallerBuilder;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessJobCaller;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.resource.DefaultResourceOperatorBuilder;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.oceanbase.odc.service.task.resource.K8sResourceContext;
import com.oceanbase.odc.service.task.resource.client.K8sJobClient;
import com.oceanbase.odc.service.task.resource.client.K8sJobClientSelector;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * use local process to mock k8s command
 * 
 * @author longpeng.zlp
 * @date 2024/8/28 11:18
 */
public class LocalMockK8sJobClient implements K8sJobClientSelector {
    @Override
    public K8sJobClient select(String resourceGroup) {
        return new LocalProcessClient();
    }

    private static final class LocalProcessClient implements K8sJobClient {
        @Override
        public K8sPodResource create(K8sResourceContext k8sResourceContext) throws JobException {
            JobContext jobContext = getJobContext(k8sResourceContext.getExtraData());
            ProcessJobCaller jobCaller = (ProcessJobCaller) JobCallerBuilder.buildProcessCaller(jobContext,
                    JobCallerBuilder.buildK8sEnv(jobContext));
            DefaultExecutorIdentifier executorIdentifier = (DefaultExecutorIdentifier) jobCaller.doStart(jobContext);
            return new K8sPodResource(k8sResourceContext.getRegion(), k8sResourceContext.getGroup(),
                    k8sResourceContext.type(),
                    executorIdentifier.getNamespace(),
                    executorIdentifier.getExecutorName(), ResourceState.AVAILABLE,
                    "127.0.0.1", String.valueOf(executorIdentifier.getPort()), new Date(System.currentTimeMillis()));
        }

        private JobContext getJobContext(Object extraData) {
            JobContext ret = (JobContext) extraData;
            if (null == ret) {
                DefaultJobContext jobContext = new DefaultJobContext();
                jobContext.setJobClass(DummyTask.class.getName());
                JobIdentity jobEntity = new JobIdentity();
                jobEntity.setId(1L);
                jobContext.setJobIdentity(jobEntity);
                ret = jobContext;
            }
            return ret;
        }


        @Override
        public Optional<K8sPodResource> get(String namespace, String arn) throws JobException {
            K8sPodResource ret = new K8sPodResource(ResourceIDUtil.DEFAULT_REGION_PROP_NAME,
                    ResourceIDUtil.DEFAULT_GROUP_PROP_NAME, DefaultResourceOperatorBuilder.CLOUD_K8S_POD_TYPE,
                    namespace, arn, ResourceState.AVAILABLE,
                    "127.0.0.1", "8989", new Date(System.currentTimeMillis()));
            return Optional.of(ret);
        }

        @Override
        public String delete(String namespace, String arn) throws JobException {
            long pid = Long.parseLong(namespace);
            SystemUtils.killProcessByPid(pid);
            return namespace + ":" + arn;
        }
    }
}
