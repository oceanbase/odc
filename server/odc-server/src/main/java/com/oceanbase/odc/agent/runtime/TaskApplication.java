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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import com.oceanbase.odc.common.ExitHelper;
import com.oceanbase.odc.common.JobContextResolver;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 15:33
 */
@Slf4j
public class TaskApplication {

    private JobContext context;

    public void run(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Task executor exits, systemInfo={}", SystemUtils.getSystemMemoryInfo());
        }));
        try {
            context = new JobContextResolver().resolveJobContext(args);
            // set log4j xml
            setLog4JConfigXml();
            log.info("context is {}", JsonUtils.toJson(context));
            log.info("initial log configuration success.");
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
}
