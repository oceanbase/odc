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
package com.oceanbase.odc.service.onlineschemachange.oscfms;

import java.util.function.Supplier;

import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.fsm.ActionContext;
import com.oceanbase.odc.service.onlineschemachange.model.LinkType;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;

import lombok.Data;

/**
 * context for osc action
 * 
 * @author longpeng.zlp
 * @date 2024/7/8 11:11
 * @since 4.3.1
 */
@Data
public class OSCActionContext implements ActionContext {
    private Long projectId;

    private ScheduleEntity schedule;

    private ScheduleTaskEntity scheduleTask;

    private LinkType linkType;

    private OnlineSchemaChangeParameters parameter;

    private OnlineSchemaChangeScheduleTaskParameters taskParameter;

    /**
     * schedule task update interface
     */
    private ScheduleTaskRepository scheduleTaskRepository;

    /**
     * connector config supplier for OMS task operation
     */
    private Supplier<ConnectionConfig> connectionConfigSupplier;
}
