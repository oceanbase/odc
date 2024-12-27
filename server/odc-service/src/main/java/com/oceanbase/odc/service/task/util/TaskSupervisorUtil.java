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
package com.oceanbase.odc.service.task.util;

import org.springframework.boot.autoconfigure.web.ServerProperties;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/12/2 10:50
 */
@Slf4j
public class TaskSupervisorUtil {
    public static SupervisorEndpoint getDefaultSupervisorEndpoint() {
        String host = SystemUtils.getLocalIpAddress();
        ServerProperties serverProperties = SpringContextUtil.getBean(ServerProperties.class);
        int port = serverProperties.getPort();
        return new SupervisorEndpoint(host, (port + 1000) % 65535);
    }

    /**
     * current task supervisor agent
     * 
     * @param taskFrameworkProperties
     * @return
     */
    public static boolean isTaskSupervisorEnabled(TaskFrameworkProperties taskFrameworkProperties) {
        return (taskFrameworkProperties.isEnableTaskSupervisorAgent());
    }

    public static JobContext buildJobContextFromJobEntity(JobEntity jobEntity) {
        return new DefaultJobContextBuilder().build(jobEntity);
    }
}
