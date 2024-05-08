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

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobCallerBuilder;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.provider.JobImageNameProvider;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

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
        JobCaller jobCaller = getJobCallerWithContext(getJobRunMode(context.getJobIdentity()), context);
        jobCaller.start(context);
    }

    @Override
    public void stop(JobIdentity ji) throws JobException {
        JobCaller jobCaller = getJobCaller(getJobRunMode(ji));
        jobCaller.stop(ji);
    }

    @Override
    public void modify(JobIdentity ji, String jobParametersJson) throws JobException {
        JobCaller jobCaller = getJobCaller(getJobRunMode(ji));
        jobCaller.modify(ji, jobParametersJson);
    }

    @Override
    public void destroy(JobIdentity ji) throws JobException {
        JobCaller jobCaller = getJobCaller(getJobRunMode(ji));
        jobCaller.destroy(ji);
    }

    private JobCaller getJobCaller(TaskRunMode taskRunMode) {
        return getJobCallerWithContext(taskRunMode, null);
    }

    private JobCaller getJobCallerWithContext(TaskRunMode taskRunMode, JobContext context) {
        JobConfiguration config = JobConfigurationHolder.getJobConfiguration();
        if (taskRunMode == TaskRunMode.K8S) {
            return JobCallerBuilder.buildK8sJobCaller(config.getK8sJobClient(),
                    createDefaultPodConfig(config.getTaskFrameworkProperties()), context);
        }
        return JobCallerBuilder.buildProcessCaller(context);
    }

    private TaskRunMode getJobRunMode(JobIdentity ji) {
        JobConfigurationValidator.validComponent();
        TaskFrameworkService taskFrameworkService =
                JobConfigurationHolder.getJobConfiguration().getTaskFrameworkService();
        return taskFrameworkService.find(ji.getId()).getRunMode();
    }

    private PodConfig createDefaultPodConfig(TaskFrameworkProperties taskFrameworkProperties) {
        K8sProperties k8s = taskFrameworkProperties.getK8sProperties();

        PodConfig podConfig = new PodConfig();
        if (StringUtils.isNotBlank(k8s.getNamespace())) {
            podConfig.setNamespace(k8s.getNamespace());
        }
        JobImageNameProvider jobImageNameProvider = JobConfigurationHolder.getJobConfiguration()
                .getJobImageNameProvider();
        podConfig.setImage(jobImageNameProvider.provide());
        podConfig.setRegion(StringUtils.isNotBlank(k8s.getRegion()) ? k8s.getRegion()
                : SystemUtils.getEnvOrProperty(JobEnvKeyConstants.OB_ARN_PARTITION));

        podConfig.setRequestCpu(k8s.getRequestCpu());
        podConfig.setRequestMem(k8s.getRequestMem());
        podConfig.setLimitCpu(k8s.getLimitCpu());
        podConfig.setLimitMem(k8s.getLimitMem());
        podConfig.setEnableMount(k8s.getEnableMount());
        podConfig.setMountPath(
                StringUtils.isNotBlank(k8s.getMountPath()) ? k8s.getMountPath()
                        : JobConstants.ODC_EXECUTOR_DEFAULT_MOUNT_PATH);
        podConfig.setMountDiskSize(k8s.getMountDiskSize());
        podConfig.setMaxNodeCount(k8s.getMaxNodeCount());
        podConfig.setNodeCpu(k8s.getNodeCpu());
        podConfig.setNodeMemInMB(k8s.getNodeMemInMB());
        podConfig.setPodPendingTimeoutSeconds(k8s.getPodPendingTimeoutSeconds());
        return podConfig;
    }

}
