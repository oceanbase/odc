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

import java.util.Date;
import java.util.Map;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskMonitorMode;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.jasypt.JasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.resource.PodConfig;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisor;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
public class JobCallerBuilder {

    /**
     * build process caller with given env
     * 
     * @param context
     * @param environments env for process builder
     * @return
     */
    public static ProcessJobCaller buildProcessCaller(JobContext context, Map<String, String> environments) {
        return buildProcessCaller(context, environments, JobConfigurationHolder.getJobConfiguration());
    }

    public static ProcessJobCaller buildProcessCaller(JobContext context, Map<String, String> environments,
            JobConfiguration configuration) {
        JobUtils.encryptEnvironments(environments);
        setReportMode(environments, context);
        ProcessConfig config = new ProcessConfig();
        config.setEnvironments(environments);
        TaskFrameworkProperties taskFrameworkProperties =
                configuration.getTaskFrameworkProperties();
        config.setJvmXmsMB(taskFrameworkProperties.getJobProcessMinMemorySizeInMB());
        config.setJvmXmxMB(taskFrameworkProperties.getJobProcessMaxMemorySizeInMB());
        String mainClassName = StringUtils.isBlank(taskFrameworkProperties.getProcessMainClassName())
                ? JobConstants.ODC_AGENT_CLASS_NAME
                : taskFrameworkProperties.getProcessMainClassName();
        TaskSupervisor taskSupervisor = new TaskSupervisor(new SupervisorEndpoint(SystemUtils.getLocalIpAddress(),
                DefaultExecutorIdentifier.DEFAULT_PORT), JobUtils.getODCServerPort(configuration), mainClassName);
        return new ProcessJobCaller(config, taskSupervisor);
    }

    /**
     * build k8s start env
     * 
     * @param context
     * @return
     */
    public static Map<String, String> buildK8sEnv(JobContext context, String logPath) {
        Map<String, String> environments = new JobEnvironmentFactory().build(context, TaskRunMode.K8S, logPath);

        Map<String, String> jobProperties = context.getJobProperties();

        // executor listen port
        int executorListenPort = JobPropertiesUtils.getExecutorListenPort(jobProperties);
        if (executorListenPort > 0) {
            environments.put(JobEnvKeyConstants.ODC_EXECUTOR_PORT, String.valueOf(executorListenPort));
        }

        setReportMode(environments, context);

        // encryption related properties
        JasyptEncryptorConfigProperties jasyptProperties = JobConfigurationHolder.getJobConfiguration()
                .getJasyptEncryptorConfigProperties();

        environments.put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM, jasyptProperties.getAlgorithm());
        environments.put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_PREFIX, jasyptProperties.getPrefix());
        environments.put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SUFFIX, jasyptProperties.getSuffix());
        environments.put(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SALT, jasyptProperties.getSalt());
        return environments;
    }

    private static void setReportMode(Map<String, String> environments, JobContext jobContext) {
        TaskMonitorMode monitorMode = JobPropertiesUtils.getMonitorMode(jobContext.getJobProperties());
        if (TaskMonitorMode.PULL.equals(monitorMode)) {
            environments.put(JobEnvKeyConstants.REPORT_ENABLED, "false");
        } else {
            environments.put(JobEnvKeyConstants.REPORT_ENABLED, "true");
        }
    }

    public static JobCaller buildK8sJobCaller(PodConfig podConfig, JobContext context,
            ResourceManager resourceManager, String resourceType, Date jobCreateTime) {
        Map<String, String> environments = buildK8sEnv(context, podConfig.getMountPath());
        // common environment variables
        environments.put(JobEnvKeyConstants.ODC_LOG_DIRECTORY, podConfig.getMountPath());
        // do encryption for sensitive information
        JobUtils.encryptEnvironments(environments);

        podConfig.setEnvironments(environments);
        return new K8sJobCaller(podConfig, resourceManager, resourceType, jobCreateTime);
    }
}
