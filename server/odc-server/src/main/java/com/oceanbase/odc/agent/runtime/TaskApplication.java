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

import com.oceanbase.odc.agent.OdcAgent;
import com.oceanbase.odc.common.BootAgentUtil;
import com.oceanbase.odc.common.ExitHelper;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.caller.JobContext;

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
            context = new BootAgentUtil().resolveJobContext(args);
            // set log4j xml, need env set by resolveJobContext
            BootAgentUtil.setLog4JConfigXml(OdcAgent.class.getClassLoader(), "log4j2-task.xml");
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
}
