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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.trace.TaskContextHolder;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.context.JobContextProviderFactory;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.executor.server.EmbedServer;
import com.oceanbase.odc.service.task.executor.server.ExitHelper;
import com.oceanbase.odc.service.task.executor.server.TaskExecutor;
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

    private TaskExecutor taskExecutor;
    private JobContext context;

    public void run(String[] args) {
        StopWatch watch = StopWatch.createStarted();
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(TaskExecutorConfig.class);
        ctx.refresh();
        DataMaskingService bean = ctx.getBean(DataMaskingService.class);
        log.info("Get bean {} from spring, mask enable is {}.", bean.getClass().getSimpleName(),
                bean.isMaskingEnabled());
        watch.stop();
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
            log.info("Task created {}.", JsonUtils.toJson(context.getJobIdentity()));
            taskExecutor.execute(task, context);
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
        context = JobContextProviderFactory.create().provide();
        trace(context.getJobIdentity().getId());
        setLog4JConfigXml();
        log.info("Log task id is {}.", context.getJobIdentity().getId());

        System.setProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY, LogUtils.getBaseLogPath());
        log.info("Log directory is {}.", LogUtils.getBaseLogPath());

        String runMode = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_TASK_RUN_MODE);
        log.info("ODC TASK RUN MODE is {}.", runMode);
        Verify.notBlank(runMode, JobEnvKeyConstants.ODC_TASK_RUN_MODE);

        taskExecutor = ThreadPoolTaskExecutor.getInstance();
        log.info("Task executor init success: {}", taskExecutor.getClass().getSimpleName());
        log.info("Task executor ip is {}.", SystemUtils.getLocalIpAddress());
        log.info("Task executor port is {}.", JobUtils.getExecutorPort());
    }

    private void trace(long taskId) {
        TraceContextHolder.trace();
        // mock userId
        TaskContextHolder.trace(1L, taskId);
    }

    private void setLog4JConfigXml() {
        String taskLogFile = "log4j2-task-executor.xml";
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);

        URL resource = getClass().getClassLoader().getResource(taskLogFile);
        try {
            // this will force a reconfiguration
            context.setConfigLocation(resource.toURI());
        } catch (URISyntaxException e) {
            throw new TaskRuntimeException("load " + taskLogFile + " occur error.", e);
        }
    }

}
