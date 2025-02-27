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
package com.oceanbase.odc.supervisor.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisor;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.runtime.LocalTaskCommandExecutor;
import com.oceanbase.odc.service.task.supervisor.runtime.TaskSupervisorServer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/11/26 14:41
 */
@Slf4j
public class SupervisorApplication {
    @Getter
    private TaskSupervisorServer taskSupervisorServer;
    private final int port;
    private AtomicBoolean stopped = new AtomicBoolean(false);

    public SupervisorApplication(int port) {
        this.port = port;
    }

    public void start(String[] args) {
        // TODO(longxuan): will be given in future release
        TaskSupervisor taskSupervisor =
                new TaskSupervisor(new SupervisorEndpoint(SystemUtils.getLocalIpAddress(), port),
                        JobConstants.ODC_AGENT_CLASS_NAME);
        taskSupervisorServer = new TaskSupervisorServer(port, new LocalTaskCommandExecutor(taskSupervisor));
        try {
            taskSupervisorServer.start();
            log.info("Starting supervisor agent.");
            // current directly quit agent
        } catch (Exception e) {
            log.warn("Supervisor agent stopped", e);
            stopped.set(true);
        }
    }

    public void waitStop() {
        try {
            if (stopped.get()) {
                return;
            }
            if (null != taskSupervisorServer) {
                taskSupervisorServer.waitStop();
            }
        } catch (Exception e) {
            log.warn("Stop supervisor agent occur exception:", e);
        }
    }

    public void stop() {
        stopped.set(true);
        if (null != taskSupervisorServer) {
            try {
                taskSupervisorServer.stop();
            } catch (Throwable e) {
                log.warn("Stop supervisor agent occur exception:", e);
            }
        }
    }
}
