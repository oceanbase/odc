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

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.jasypt.JasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunMode;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
public class JobCallerBuilder {

    public static JobCaller buildProcessCaller(JobContext context) {
        ProcessConfig config = new ProcessConfig();
        Map<String, String> environments = new JobEnvironmentFactory().getEnvironments(context, TaskRunMode.PROCESS);
        new JobEnvironmentEncryptor().encrypt(environments);
        config.setEnvironments(environments);
        TaskFrameworkProperties taskFrameworkProperties =
                JobConfigurationHolder.getJobConfiguration().getTaskFrameworkProperties();
        config.setJvmXmsMB(taskFrameworkProperties.getJobProcessMinMemorySizeInMB());
        config.setJvmXmxMB(taskFrameworkProperties.getJobProcessMaxMemorySizeInMB());

        return new ProcessJobCaller(config);
    }

    public static JobCaller buildK8sJobCaller(K8sJobClient k8sJobClient, PodConfig podConfig, JobContext context) {

        Map<String, String> environments = new JobEnvironmentFactory().getEnvironments(context, TaskRunMode.K8S);
        podConfig.getEnvironments().put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SALT,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SALT));
        new JobEnvironmentEncryptor().encrypt(environments);

        JasyptEncryptorConfigProperties configProperties = JobConfigurationHolder.getJobConfiguration()
            .getJasyptEncryptorConfigProperties();
        podConfig.setEnvironments(environments);
        podConfig.getEnvironments().put(JobEnvKeyConstants.ODC_LOG_DIRECTORY, podConfig.getMountPath());
        podConfig.getEnvironments().put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM,
            configProperties.getAlgorithm());
        podConfig.getEnvironments().put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_PREFIX,
            configProperties.getPrefix());
        podConfig.getEnvironments().put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SUFFIX,
            configProperties.getSuffix());
        podConfig.getEnvironments().put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SALT,
            configProperties.getSalt());
        return new K8sJobCaller(k8sJobClient, podConfig);
    }
}
