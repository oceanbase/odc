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

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.common.util.StringUtils;
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
        /**
         * write JobContext to file in case of exceeding the environments size limit; set the file path in
         * the environment instead
         */
        String jobContextFilePath = JobUtils.getExecutorDataPath() + "/" + StringUtils.uuid() + ".enc";
        try {
            FileUtils.writeStringToFile(new File(jobContextFilePath),
                    environments.get(JobEnvKeyConstants.ODC_JOB_CONTEXT),
                    Charset.defaultCharset());
        } catch (Exception ex) {
            FileUtils.deleteQuietly(new File(jobContextFilePath));
            throw new RuntimeException("Failed to write job context to file: " + jobContextFilePath, ex);
        }
        environments.put(JobEnvKeyConstants.ODC_JOB_CONTEXT_FILE_PATH,
                JobUtils.encrypt(environments.get(JobEnvKeyConstants.ENCRYPT_KEY),
                        environments.get(JobEnvKeyConstants.ENCRYPT_SALT), jobContextFilePath));
        // remove JobContext from environments
        environments.remove(JobEnvKeyConstants.ODC_JOB_CONTEXT);
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
            environments.put(JobEnvKeyConstants.REPORT_ENABLED, "false");
        } else {
            environments.put(JobEnvKeyConstants.REPORT_ENABLED, "true");
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
