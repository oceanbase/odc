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

package com.oceanbase.odc.service.task.dispatch;

import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobCallerBuilder;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.caller.PodParam;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.provider.JobImageNameProvider;

/**
 * Dispatch job to JobCaller immediately
 *
 * @author yaobin
 * @date 2023-11-20
 * @since 4.2.4
 */
public class ImmediateJobDispatcher implements JobDispatcher {

    @Override
    public void start(JobContext context) throws JobException {

        JobCaller jobCaller = getJobCaller(JobConfigurationHolder.getJobConfiguration());
        jobCaller.start(context);
    }

    @Override
    public void stop(JobIdentity ji) throws JobException {
        JobCaller jobCaller = getJobCaller(JobConfigurationHolder.getJobConfiguration());
        jobCaller.stop(ji);
    }

    @Override
    public void destroy(JobIdentity ji) throws JobException {
        JobCaller jobCaller = getJobCaller(JobConfigurationHolder.getJobConfiguration());
        jobCaller.destroy(ji);
    }

    @Override
    public void destroy(ExecutorIdentifier executorIdentifier) throws JobException {
        JobCaller jobCaller = getJobCaller(JobConfigurationHolder.getJobConfiguration());
        jobCaller.destroy(executorIdentifier);
    }

    private JobCaller getJobCaller(JobConfiguration config) {
        if (config.getTaskFrameworkProperties().getRunMode() == TaskRunModeEnum.K8S) {
            return JobCallerBuilder.buildK8sJobCaller(config.getK8sJobClient(),
                    createDefaultPodConfig(config.getTaskFrameworkProperties()));
        }
        return JobCallerBuilder.buildJvmCaller();
    }

    private PodConfig createDefaultPodConfig(TaskFrameworkProperties taskFrameworkProperties) {
        K8sProperties k8s = taskFrameworkProperties.getK8s();

        PodConfig podConfig = new PodConfig();
        podConfig.setNamespace(k8s.getNamespace());
        JobImageNameProvider jobImageNameProvider = JobConfigurationHolder.getJobConfiguration()
                .getJobImageNameProvider();
        podConfig.setImage(jobImageNameProvider.provide());

        PodParam podParam = podConfig.getPodParam();
        podParam.setRequestCpu(k8s.getRequestCpu());
        podParam.setRequestMem(k8s.getRequestMem());
        podParam.setLimitCpu(k8s.getLimitCpu());
        podParam.setLimitMem(k8s.getLimitMem());
        podParam.setEnableMount(k8s.getEnableMount());
        podParam.setMountPath(k8s.getMountPath());
        podParam.setMountDiskSize(k8s.getMountDiskSize());
        return podConfig;
    }

}
