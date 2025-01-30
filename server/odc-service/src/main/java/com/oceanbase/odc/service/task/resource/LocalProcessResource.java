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
package com.oceanbase.odc.service.task.resource;

import java.util.Date;
import java.util.Optional;

import com.oceanbase.odc.metadb.task.SupervisorEndpointEntity;
import com.oceanbase.odc.metadb.task.SupervisorEndpointRepository;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.supervisor.SupervisorEndpointState;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisor;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.runtime.LocalTaskCommandExecutor;
import com.oceanbase.odc.service.task.supervisor.runtime.TaskSupervisorServer;
import com.oceanbase.odc.service.task.util.TaskSupervisorUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * process agent server for odc if task is started with supervisor agent in process mode
 * 
 * @author longpeng.zlp
 * @date 2024/12/18 14:58
 */
@Slf4j
public class LocalProcessResource {
    protected final SupervisorEndpointRepository supervisorEndpointRepository;
    protected TaskSupervisorServer taskSupervisorServer;

    public LocalProcessResource(SupervisorEndpointRepository supervisorEndpointRepository) {
        this.supervisorEndpointRepository = supervisorEndpointRepository;
    }

    public void prepareLocalProcessResource() {
        SupervisorEndpoint localEndpoint = TaskSupervisorUtil.getDefaultSupervisorEndpoint();
        startTaskSupervisorServer(localEndpoint);
        tryRegisterTaskSupervisorAgent(localEndpoint);
    }

    private void startTaskSupervisorServer(SupervisorEndpoint supervisorEndpoint) {
        TaskSupervisor taskSupervisor =
                new TaskSupervisor(supervisorEndpoint,
                        JobConstants.ODC_AGENT_CLASS_NAME);
        taskSupervisorServer =
                new TaskSupervisorServer(supervisorEndpoint.getPort(), new LocalTaskCommandExecutor(taskSupervisor));
        try {
            taskSupervisorServer.start();
            log.info("Starting task supervisor server.");
            // current directly quit agent
        } catch (Exception e) {
            log.warn("Supervisor agent stopped", e);
            throw e;
        }
    }

    /**
     * register self to meta store
     */
    private void tryRegisterTaskSupervisorAgent(SupervisorEndpoint localEndpoint) {
        log.info("start with supervisor agent mode, try register agent");
        Optional<SupervisorEndpointEntity> registered = supervisorEndpointRepository
                .findByHostPortAndResourceId(localEndpoint.getHost(), localEndpoint.getPort(), -1L);
        if (registered.isPresent()) {
            supervisorEndpointRepository.updateStatusByHostPortAndResourceId(localEndpoint.getHost(),
                    localEndpoint.getPort(), -1L,
                    SupervisorEndpointState.AVAILABLE.name());
        } else {
            SupervisorEndpointEntity created = new SupervisorEndpointEntity();
            created.setHost(localEndpoint.getHost());
            created.setPort(localEndpoint.getPort());
            created.setResourceID(-1L);
            created.setLoads(0);
            created.setResourceGroup(ResourceIDUtil.PROCESS_RESOURCE_LOCATION.getGroup());
            created.setResourceRegion(ResourceIDUtil.PROCESS_RESOURCE_LOCATION.getRegion());
            created.setStatus(SupervisorEndpointState.AVAILABLE.name());
            created.setCreateTime(new Date(System.currentTimeMillis()));
            supervisorEndpointRepository.save(created);
        }
    }
}
