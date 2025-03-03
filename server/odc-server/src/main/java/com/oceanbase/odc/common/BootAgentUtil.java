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
package com.oceanbase.odc.common;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import com.oceanbase.odc.common.trace.TaskContextHolder;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobEnvironmentEncryptor;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.context.JobContextProviderFactory;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/11/26 15:11
 */
@Slf4j
public class BootAgentUtil {
    public JobContext resolveJobContext(String[] args) {
        // 1 step: valid environment value not blank
        validEnvValues();
        log.info("verify environment variables success.");

        // 2 step: decrypt environment value
        decryptEnvironments();
        log.info("decrypt environment variables success.");

        // 3 step: get JobContext from environment
        JobContext context =
                JobContextProviderFactory.create(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE))
                        .provide();
        log.info("initial job context success.");

        // 4 step: trace taskId in log4j2 context
        trace(context.getJobIdentity().getId());
        // 5 step: set log path in system properties
        setLogPathSysProperty();

        log.info("Task executor start info, ip={}, port={}, runMode={}, taskId={}, logPath={}, userId={}.",
                SystemUtils.getLocalIpAddress(),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE),
                context.getJobIdentity().getId(),
                System.getProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID));
        return context;
    }

    private void decryptEnvironments() {
        Map<String, String> allProperties = new HashMap<>(System.getenv());
        System.getProperties().forEach((key, value) -> {
            allProperties.put((String) key, (String) value);
        });
        new JobEnvironmentEncryptor().decrypt(allProperties);
    }

    private void trace(long taskId) {
        TraceContextHolder.trace();
        // mock userId
        TaskContextHolder.trace(JobUtils.getUserId(), taskId);
    }

    public static void setLogPathSysProperty() {
        JobUtils.putEnvToSysProperties(JobEnvKeyConstants.ODC_LOG_DIRECTORY);
    }

    private void validEnvValues() {
        validNotBlank(JobEnvKeyConstants.ODC_TASK_RUN_MODE);
        if (StringUtils.equalsIgnoreCase("PROCESS",
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE))) {
            validNotBlank(JobEnvKeyConstants.ODC_JOB_CONTEXT_FILE_PATH);
        } else {
            validNotBlank(JobEnvKeyConstants.ODC_JOB_CONTEXT);
        }
        validNotBlank(JobEnvKeyConstants.ODC_BOOT_MODE);
        validNotBlank(JobEnvKeyConstants.ENCRYPT_SALT);
        validNotBlank(JobEnvKeyConstants.ENCRYPT_KEY);
        validNotBlank(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID);
        validNotBlank(JobEnvKeyConstants.ODC_LOG_DIRECTORY);
    }

    private void validNotBlank(String envKey) {
        Verify.notBlank(SystemUtils.getEnvOrProperty(envKey), envKey);
    }

    public static void setLog4JConfigXml(ClassLoader classLoader, String candidateLogConfName) {
        String configurationFile = System.getProperty("log4j.configurationFile");
        URI taskLogFile = null;
        if (configurationFile != null) {
            File file = new File(configurationFile);
            if (file.exists() && file.isFile()) {
                taskLogFile = file.toURI();
            }
        }
        if (taskLogFile == null) {
            try {
                taskLogFile = classLoader.getResource(candidateLogConfName).toURI();
            } catch (URISyntaxException e) {
                throw new TaskRuntimeException("load default " + candidateLogConfName + " occur error:", e);
            }
        }

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        // this will force a reconfiguration, MDC context will to take effect
        context.setConfigLocation(taskLogFile);
    }

}
