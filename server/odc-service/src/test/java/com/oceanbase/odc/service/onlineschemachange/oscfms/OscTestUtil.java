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

import java.util.Arrays;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.model.LinkType;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.model.SwapTableType;

/**
 * util to build test resources
 * 
 * @author longpeng.zlp
 * @date 2024/7/25 15:22
 * @since 4.3.1
 */
public class OscTestUtil {
    /**
     * create task parameters
     */
    public static OnlineSchemaChangeScheduleTaskParameters createTaskParameters(DialectType dialectType, String state) {
        OnlineSchemaChangeScheduleTaskParameters taskParameters = new OnlineSchemaChangeScheduleTaskParameters();
        taskParameters.setUid("testUid");
        taskParameters.setDialectType(dialectType);
        taskParameters.setState(state);
        taskParameters.setDatabaseName("testDB");
        taskParameters.setLinkType(LinkType.OMS);
        taskParameters.setSqlsToBeExecuted(Arrays.asList(
                "set time_zone = '+08:00'"));
        taskParameters.setNewTableName("ghost_test_table");
        taskParameters.setOriginTableName("test_table");
        taskParameters.setRenamedTableName("old_test_table");
        taskParameters.setOriginTableCreateDdl("create table `test_table`("
                + " id int not null primary key,"
                + " name varchar(20)"
                + ")");
        taskParameters.setNewTableCreateDdl("create table `ghost_test_table`("
                + " id int not null primary key,"
                + " name varchar(20),"
                + " age int"
                + ")");
        return taskParameters;
    }

    /**
     * create task result
     */
    public static OnlineSchemaChangeScheduleTaskResult createTaskResult(DialectType dialectType) {
        OnlineSchemaChangeScheduleTaskResult taskResult = new OnlineSchemaChangeScheduleTaskResult();
        taskResult.setDialectType(dialectType);
        return taskResult;
    }

    /**
     * create schedule task entity
     */
    public static ScheduleTaskEntity createScheduleTaskEntity(TaskStatus taskStatus) {
        ScheduleTaskEntity taskEntity = new ScheduleTaskEntity();
        taskEntity.setId(1L);
        taskEntity.setStatus(taskStatus);
        return taskEntity;
    }

    /**
     * create schedule entity
     */
    public static ScheduleEntity createScheduleEntity() {
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setId(100L);
        return scheduleEntity;
    }

    /**
     * create schedule entity
     */
    public static OnlineSchemaChangeParameters createOscParameters() {
        OnlineSchemaChangeParameters parameters = new OnlineSchemaChangeParameters();
        parameters.setLockUsers(Arrays.asList("user1", "user2"));
        parameters.setDelimiter(";");
        parameters.setSqlType(OnlineSchemaChangeSqlType.ALTER);
        parameters.setSwapTableType(SwapTableType.MANUAL);
        return parameters;
    }

    public static OscActionContext createOcsActionContext(DialectType dialectType, String state,
            TaskStatus taskStatus) {
        OnlineSchemaChangeScheduleTaskParameters taskParameters = createTaskParameters(dialectType, state);
        OnlineSchemaChangeScheduleTaskResult taskResult = createTaskResult(dialectType);
        OscActionContext oscActionContext = new OscActionContext();
        oscActionContext.setParameter(createOscParameters());
        oscActionContext.setLinkType(LinkType.OMS);
        oscActionContext.setSchedule(createScheduleEntity());
        ScheduleTaskEntity scheduleTaskEntity = createScheduleTaskEntity(taskStatus);
        scheduleTaskEntity.setResultJson(JsonUtils.toJson(taskResult));
        oscActionContext.setScheduleTask(scheduleTaskEntity);
        oscActionContext.setProjectId(1024L);
        oscActionContext.setTaskParameter(taskParameters);
        return oscActionContext;
    }
}
