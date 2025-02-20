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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.caller.ExecutorInfo;
import com.oceanbase.odc.service.task.caller.JobCallerBuilder;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.caller.ProcessJobCaller;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.resource.AbstractK8sResourceOperatorBuilder;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.oceanbase.odc.service.task.resource.K8sResourceContext;
import com.oceanbase.odc.service.task.resource.PodConfig;
import com.oceanbase.odc.service.task.resource.client.K8sJobClient;
import com.oceanbase.odc.service.task.resource.client.K8sJobClientSelector;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisor;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

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
        private final TaskSupervisor taskSupervisor;

        public LocalProcessClient() {
            taskSupervisor = new TaskSupervisor(new SupervisorEndpoint(SystemUtils.getLocalIpAddress(), 8989),
                    "com.oceanbase.odc.supervisor.SupervisorAgent");
            taskSupervisor.setJobInfoSerializer(null);
        }

        @Override
        public K8sPodResource create(K8sResourceContext k8sResourceContext) throws JobException {
            JobContext jobContext = getJobContext(k8sResourceContext.getExtraData());
            if (null != jobContext) {
                // normal process
                ProcessJobCaller jobCaller = JobCallerBuilder.buildProcessCaller(jobContext,
                        JobCallerBuilder.buildK8sEnv(jobContext, LogUtils.getBaseLogPath()));
                ExecutorInfo executorInfo = jobCaller.doStart(
                        jobContext);
                ExecutorIdentifier executorIdentifier = executorInfo.getExecutorIdentifier();

                return new K8sPodResource(k8sResourceContext.getRegion(), k8sResourceContext.getGroup(),
                        k8sResourceContext.type(),
                        executorIdentifier.getNamespace(),
                        executorIdentifier.getExecutorName(), ResourceState.AVAILABLE,
                        SystemUtils.getLocalIpAddress(), String.valueOf(executorIdentifier.getPort()),
                        new Date(System.currentTimeMillis()));
            } else {
                // supervisor
                PodConfig podConfig = k8sResourceContext.getPodConfig();
                ExecutorEndpoint executorEndpoint = startTask(podConfig.getEnvironments());
                ExecutorIdentifier executorIdentifier =
                        ExecutorIdentifierParser.parser(executorEndpoint.getIdentifier());
                return new K8sPodResource(k8sResourceContext.getRegion(), k8sResourceContext.getGroup(),
                        k8sResourceContext.type(),
                        executorIdentifier.getNamespace(),
                        executorEndpoint.getIdentifier() + "supervisor", ResourceState.AVAILABLE,
                        SystemUtils.getLocalIpAddress(),
                        podConfig.getEnvironments().get(JobEnvKeyConstants.ODC_SUPERVISOR_LISTEN_PORT),
                        new Date(System.currentTimeMillis()));
            }
        }

        private JobContext getJobContext(Object extraData) {
            JobContext ret = (JobContext) extraData;
            return ret;
        }


        @Override
        public Optional<K8sPodResource> get(String namespace, String arn) throws JobException {
            K8sPodResource ret = new K8sPodResource(ResourceIDUtil.REGION_PROP_NAME,
                    ResourceIDUtil.GROUP_PROP_NAME, AbstractK8sResourceOperatorBuilder.CLOUD_K8S_POD_TYPE,
                    namespace, arn, ResourceState.AVAILABLE,
                    SystemUtils.getLocalIpAddress(), "8989", new Date(System.currentTimeMillis()));
            return Optional.of(ret);
        }

        private ExecutorEndpoint startTask(Map<String, String> env) throws JobException {
            Map<String, String> newEvn = new HashMap<>(env);
            newEvn.put(JobEnvKeyConstants.REPORT_ENABLED, "false");
            DefaultJobContext jobContext = new DefaultJobContext();
            jobContext.setJobIdentity(JobIdentity.of(1L));
            ProcessConfig processConfig = new ProcessConfig();
            processConfig.setJvmXmxMB(512);
            processConfig.setJvmXmsMB(512);
            processConfig.setEnvironments(newEvn);
            return taskSupervisor.startTask(jobContext, processConfig);
        }

        @Override
        public String delete(String namespace, String arn) throws JobException {
            long pid = Long.parseLong(namespace);
            SystemUtils.killProcessByPid(pid);
            return namespace + ":" + arn;
        }
    }
}
