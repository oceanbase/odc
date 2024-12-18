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
package com.oceanbase.odc.supervisor;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.classification.InterfaceStability.Evolving;

import com.oceanbase.odc.common.JobContextResolver;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.server.module.Modules;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.supervisor.protocol.StartTaskCommand;
import com.oceanbase.odc.supervisor.runtime.SupervisorApplication;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/11/26 15:43
 */
@Slf4j
@Evolving
public class SupervisorAgent {
    public static void main(String[] args) {

        log.info("Supervisor agent started");
        SupervisorApplication supervisorApplication = null;
        try {
            Modules.load();
            supervisorApplication = new SupervisorApplication();
            supervisorApplication.start(args);
            // send command context, then stop to compatible with previous logic
            JobContext jobContext = new JobContextResolver().resolveJobContext(args);
            supervisorApplication.getTaskSupervisorServer().getTaskCommandExecutor()
                    .onCommand(StartTaskCommand.create(jobContext, buildFromJobContext(jobContext)));
            // supervisorApplication.waitStop();
        } catch (Throwable e) {
            log.error("Supervisor agent stopped", e);
        } finally {
            if (null != supervisorApplication) {
                supervisorApplication.stop();
            }
        }
        log.info("Supervisor agent stopped.");
    }

    private static ProcessConfig buildFromJobContext(JobContext jobContext) {
        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setJvmXmsMB(1024);
        processConfig.setJvmXmxMB(2048);
        Map<String, String> env = new HashMap<>();
        for (Map.Entry<String, String> evn : System.getenv().entrySet()) {
            // ignore job context file path
            if (StringUtils.equalsIgnoreCase(evn.getKey(), JobEnvKeyConstants.ODC_JOB_CONTEXT_FILE_PATH)) {
                continue;
            }
            env.put(evn.getKey(), evn.getValue());
        }
        processConfig.setEnvironments(env);
        log.info("process config with values = {}", processConfig.getEnvironments());
        return processConfig;
    }

}
