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

package com.oceanbase.odc.service.task.executor;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import com.oceanbase.odc.common.trace.TaskContextHolder;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobEnvironmentEncryptor;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.context.JobContextProviderFactory;
import com.oceanbase.odc.service.task.executor.server.EmbedServer;
import com.oceanbase.odc.service.task.executor.server.ExitHelper;
import com.oceanbase.odc.service.task.executor.server.TaskFactory;
import com.oceanbase.odc.service.task.executor.server.ThreadPoolTaskExecutor;
import com.oceanbase.odc.service.task.executor.task.Task;
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
        // 2 step: decrypt environment value
        decryptEnvironments();
        // 3 step: get JobContext from environment
        context = JobContextProviderFactory.create().provide();
        // 4 step: trace taskId in log4j2 context
        trace(context.getJobIdentity().getId());
        // 5 step: set log path in system properties
        setLogPathSysProperty();
        // 6 step: set log4j2.xml
        setLog4JConfigXml();

        log.info("Task executor start info, ip={}, port={}, runMode={}, taskId={}, logPath={}, userId={}.",
                SystemUtils.getLocalIpAddress(),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE),
                context.getJobIdentity().getId(),
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY),
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
        String taskLogFile = "log4j2-task.xml";
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);

        URL resource = getClass().getClassLoader().getResource(taskLogFile);
        try {
            // this will force a reconfiguration
            context.setConfigLocation(resource.toURI());
        } catch (URISyntaxException e) {
            throw new TaskRuntimeException("load log file occur error, logfile=" + taskLogFile, e);
        }
    }

    private void validEnvValues() {
        validNotBlank(JobEnvKeyConstants.ODC_JOB_CONTEXT);
        validNotBlank(JobEnvKeyConstants.ODC_BOOT_MODE);
        validNotBlank(JobEnvKeyConstants.ODC_TASK_RUN_MODE);
        validNotBlank(JobEnvKeyConstants.ENCRYPT_SALT);
        validNotBlank(JobEnvKeyConstants.ENCRYPT_KEY);
        validNotBlank(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID);
        validNotBlank(JobEnvKeyConstants.ODC_LOG_DIRECTORY);
    }

    private void validNotBlank(String envKey) {
        Verify.notBlank(SystemUtils.getEnvOrProperty(envKey), envKey);
    }

}
