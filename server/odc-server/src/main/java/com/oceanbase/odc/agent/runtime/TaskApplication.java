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
package com.oceanbase.odc.agent.runtime;

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
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobEnvironmentEncryptor;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.context.JobContextProviderFactory;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 15:33
 */
@Slf4j
public class TaskApplication {

    private JobContext context;

    public void run(String[] args) {
        try {
            init(args);
        } catch (Exception e) {
            log.warn("Init task error:", e);
            throw e;
        }
        EmbedServer server = new EmbedServer();
        try {
            server.start();
            log.info("Starting embed server.");
            Task<?> task = TaskFactory.create(context.getJobClass());
            ThreadPoolTaskExecutor.getInstance().execute(task, context);
            ExitHelper.await();
        } catch (Exception e) {
            log.warn("Execute task error:", e);
        } finally {
            try {
                server.stop();
            } catch (Exception e) {
                log.warn("Stop embed server occur exception:", e);
            }
        }
    }

    private void init(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Task executor exits, systemInfo={}", SystemUtils.getSystemMemoryInfo());
        }));
        // 1 step: valid environment value not blank
        validEnvValues();
        log.info("verify environment variables success.");

        // 2 step: decrypt environment value
        decryptEnvironments();
        log.info("decrypt environment variables success.");

        // 3 step: get JobContext from environment
        context = JobContextProviderFactory.create(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE))
                .provide();
        log.info("initial job context success.");

        // 4 step: trace taskId in log4j2 context
        trace(context.getJobIdentity().getId());
        // 5 step: set log path in system properties
        setLogPathSysProperty();
        // 6 step: set log4j2.xml
        setLog4JConfigXml();
        log.info("initial log configuration success.");

        log.info("Task executor start info, ip={}, port={}, runMode={}, taskId={}, logPath={}, userId={}.",
                SystemUtils.getLocalIpAddress(),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE),
                context.getJobIdentity().getId(),
                System.getProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID));
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

    private void setLogPathSysProperty() {
        JobUtils.putEnvToSysProperties(JobEnvKeyConstants.ODC_LOG_DIRECTORY);
    }

    private void setLog4JConfigXml() {
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
                taskLogFile = getClass().getClassLoader().getResource("log4j2-task.xml").toURI();
            } catch (URISyntaxException e) {
                throw new TaskRuntimeException("load default log4j2-task.xml occur error:", e);
            }
        }

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        // this will force a reconfiguration, MDC context will to take effect
        context.setConfigLocation(taskLogFile);
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

}
