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
package com.oceanbase.odc.service.task.caller;

import java.util.Map;

import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskMonitorMode;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.jasypt.JasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
public class JobCallerBuilder {

    public static JobCaller buildProcessCaller(JobContext context) {
        Map<String, String> environments = new JobEnvironmentFactory().build(context, TaskRunMode.PROCESS);
        JobUtils.encryptEnvironments(environments);

        ProcessConfig config = new ProcessConfig();
        config.setEnvironments(environments);

        TaskFrameworkProperties taskFrameworkProperties =
                JobConfigurationHolder.getJobConfiguration().getTaskFrameworkProperties();
        config.setJvmXmsMB(taskFrameworkProperties.getJobProcessMinMemorySizeInMB());
        config.setJvmXmxMB(taskFrameworkProperties.getJobProcessMaxMemorySizeInMB());

        return new ProcessJobCaller(config);
    }

    public static JobCaller buildK8sJobCaller(K8sJobClient k8sJobClient, PodConfig podConfig, JobContext context) {
        Map<String, String> environments = new JobEnvironmentFactory().build(context, TaskRunMode.K8S);

        // common environment variables
        environments.put(JobEnvKeyConstants.ODC_LOG_DIRECTORY, podConfig.getMountPath());

        Map<String, String> jobProperties = context.getJobProperties();

        // executor listen port
        int executorListenPort = JobPropertiesUtils.getExecutorListenPort(jobProperties);
        if (executorListenPort > 0) {
            environments.put(JobEnvKeyConstants.ODC_EXECUTOR_PORT, String.valueOf(executorListenPort));
        }

        TaskMonitorMode monitorMode = JobPropertiesUtils.getMonitorMode(jobProperties);
        if (TaskMonitorMode.PULL.equals(monitorMode)) {
            environments.put(JobEnvKeyConstants.REPORT_ENABLED, "true");
        } else {
            environments.put(JobEnvKeyConstants.REPORT_ENABLED, "false");
        }

        // encryption related properties
        JasyptEncryptorConfigProperties jasyptProperties = JobConfigurationHolder.getJobConfiguration()
                .getJasyptEncryptorConfigProperties();

        environments.put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM, jasyptProperties.getAlgorithm());
        environments.put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_PREFIX, jasyptProperties.getPrefix());
        environments.put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SUFFIX, jasyptProperties.getSuffix());
        environments.put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SALT, jasyptProperties.getSalt());

        // do encryption for sensitive information
        JobUtils.encryptEnvironments(environments);

        podConfig.setEnvironments(environments);
        return new K8sJobCaller(k8sJobClient, podConfig);
    }
}
