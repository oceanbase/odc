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

import java.util.Map;
import java.util.Objects;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobCallerBuilder;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobEnvironmentFactory;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.resource.AbstractK8sResourceOperatorBuilder;
import com.oceanbase.odc.service.task.resource.DefaultNativeK8sOperatorBuilder;
import com.oceanbase.odc.service.task.resource.PodConfig;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.provider.JobImageNameProvider;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;

/**
 * Dispatch job to JobCaller immediately
 *
 * @author yaobin
 * @date 2023-11-20
 * @since 4.2.4
 */
public class ImmediateJobDispatcher implements JobDispatcher {
    private final ResourceManager resourceManager;

    public ImmediateJobDispatcher(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public void start(JobContext context) throws JobException {
        JobCaller jobCaller = getJobCaller(context.getJobIdentity(), context);
        jobCaller.start(context);
    }

    @Override
    public void stop(JobIdentity ji) throws JobException {
        JobCaller jobCaller = getJobCaller(ji, null);
        jobCaller.stop(ji);
    }

    @Override
    public void modify(JobIdentity ji, String jobParametersJson)
            throws JobException {
        JobCaller jobCaller = getJobCaller(ji, null);
        jobCaller.modify(ji, jobParametersJson);
    }

    @Override
    public void finish(JobIdentity ji) throws JobException {
        JobCaller jobCaller = getJobCaller(ji, null);
        jobCaller.finish(ji);
    }

    @Override
    public boolean canBeFinish(JobIdentity ji) {
        JobCaller jobCaller = getJobCaller(ji, null);
        return jobCaller.canBeFinish(ji);
    }

    private JobCaller getJobCaller(JobIdentity ji, JobContext context) {
        TaskFrameworkService taskFrameworkService =
                JobConfigurationHolder.getJobConfiguration().getTaskFrameworkService();
        JobConfiguration config = JobConfigurationHolder.getJobConfiguration();

        JobEntity je = taskFrameworkService.find(ji.getId());
        if (Objects.isNull(context)) {
            context = new DefaultJobContextBuilder().build(je);
        }
        if (je.getRunMode() == TaskRunMode.K8S) {
            PodConfig podConfig = createDefaultPodConfig(config.getTaskFrameworkProperties());
            Map<String, String> labels = JobPropertiesUtils.getLabels(context.getJobProperties());
            podConfig.setLabels(labels);
            String regionName = JobPropertiesUtils.getRegionName(context.getJobProperties());
            if (StringUtils.isNotBlank(regionName)) {
                podConfig.setRegion(regionName);
            }
            String resourceType = AbstractK8sResourceOperatorBuilder.CLOUD_K8S_POD_TYPE;
            // TODO(tianke): this is ugly code, resourceType should be dispatched before job started
            // task frame work should know where job should be dispatched to
            if (config.getTaskFrameworkProperties().isEnableK8sLocalDebugMode()) {
                resourceType = DefaultNativeK8sOperatorBuilder.NATIVE_K8S_POD_TYPE;
            }
            return JobCallerBuilder.buildK8sJobCaller(podConfig, context, resourceManager, resourceType,
                    je.getCreateTime());
        } else {
            return JobCallerBuilder.buildProcessCaller(context,
                    new JobEnvironmentFactory().build(context, TaskRunMode.PROCESS, LogUtils.getBaseLogPath()));
        }
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
