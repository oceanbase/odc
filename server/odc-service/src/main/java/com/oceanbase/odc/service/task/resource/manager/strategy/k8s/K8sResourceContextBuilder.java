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
package com.oceanbase.odc.service.task.resource.manager.strategy.k8s;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.resource.DefaultResourceOperatorBuilder;
import com.oceanbase.odc.service.task.resource.K8sResourceContext;
import com.oceanbase.odc.service.task.resource.PodConfig;
import com.oceanbase.odc.service.task.schedule.provider.JobImageNameProvider;

/**
 * @author longpeng.zlp
 * @date 2024/12/19 18:26
 */
public class K8sResourceContextBuilder {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    protected final K8sProperties k8sProperties;

    public K8sResourceContextBuilder(K8sProperties k8sProperties) {
        this.k8sProperties = k8sProperties;
    }

    public K8sResourceContext buildK8sResourceContext(Long taskID, ResourceLocation resourceLocation) {
        String jobName = generateK8sPodName(taskID);
        PodConfig podConfig = createDefaultPodConfig(k8sProperties);
        return new K8sResourceContext(podConfig, jobName, resourceLocation.getRegion(),
                resourceLocation.getGroup(),
                DefaultResourceOperatorBuilder.CLOUD_K8S_POD_TYPE, null);
    }

    private PodConfig createDefaultPodConfig(K8sProperties k8sProperties) {
        PodConfig podConfig = new PodConfig();
        if (StringUtils.isNotBlank(k8sProperties.getNamespace())) {
            podConfig.setNamespace(k8sProperties.getNamespace());
        }
        JobImageNameProvider jobImageNameProvider = JobConfigurationHolder.getJobConfiguration()
                .getJobImageNameProvider();
        podConfig.setImage(jobImageNameProvider.provide());
        podConfig.setRegion(StringUtils.isNotBlank(k8sProperties.getRegion()) ? k8sProperties.getRegion()
                : SystemUtils.getEnvOrProperty(JobEnvKeyConstants.OB_ARN_PARTITION));

        podConfig.setRequestCpu(k8sProperties.getRequestCpu());
        podConfig.setRequestMem(k8sProperties.getRequestMem());
        podConfig.setLimitCpu(k8sProperties.getLimitCpu());
        // reserve more 1024 MB for supervisor at most
        podConfig.setLimitMem(k8sProperties.getLimitMem() + 1024);
        podConfig.setEnableMount(k8sProperties.getEnableMount());
        podConfig.setMountPath(
                StringUtils.isNotBlank(k8sProperties.getMountPath()) ? k8sProperties.getMountPath()
                        : JobConstants.ODC_EXECUTOR_DEFAULT_MOUNT_PATH);
        podConfig.setMountDiskSize(k8sProperties.getMountDiskSize());
        podConfig.setMaxNodeCount(k8sProperties.getMaxNodeCount());
        podConfig.setNodeCpu(k8sProperties.getNodeCpu());
        podConfig.setNodeMemInMB(k8sProperties.getNodeMemInMB());
        podConfig.setPodPendingTimeoutSeconds(k8sProperties.getPodPendingTimeoutSeconds());
        podConfig.setEnvironments(buildEnv());
        return podConfig;
    }

    public String generateK8sPodName(long jobID) {
        return JobConstants.TEMPLATE_JOB_NAME_PREFIX + "supervisor-" + jobID + "-" + LocalDateTime.now().format(DTF);
    }

    public Map<String, String> buildEnv() {
        Map<String, String> env = new HashMap<>();
        env.put(JobEnvKeyConstants.ODC_SUPERVISOR_LISTEN_PORT,
                String.valueOf(JobConstants.JOB_POD_DEFAULT_SUPERVISOR_LISTEN_PORT));
        return env;
    }
}
