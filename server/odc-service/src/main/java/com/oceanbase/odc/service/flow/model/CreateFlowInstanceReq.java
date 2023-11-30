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
package com.oceanbase.odc.service.flow.model;

import java.util.Date;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.flow.processor.CreateFlowInstanceProcessAspect;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.OdcMockTaskConfig;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskParameter;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTaskParameters;
import com.oceanbase.odc.service.permissionapply.project.ApplyProjectParameter;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2022/2/9
 */

@Data
public class CreateFlowInstanceReq {

    /**
     * Database id
     */
    private Long databaseId;
    /**
     * Task type
     */
    @NotNull
    private TaskType taskType;
    /**
     * Execution strategy default auto.
     */
    @NotNull
    private FlowTaskExecutionStrategy executionStrategy = FlowTaskExecutionStrategy.AUTO;
    /**
     * Execution time, valid only when executionStrategy is TIMER
     */
    private Date executionTime;
    /**
     * Parent instance id, valid only when generating rollback plan
     */
    private Long parentFlowInstanceId;
    /**
     * Task description
     */
    private String description;
    /**
     * Task parameters
     */
    @NotNull
    @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "taskType")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = OdcMockTaskConfig.class, name = "MOCKDATA"),
            @JsonSubTypes.Type(value = DataTransferConfig.class, names = {"EXPORT", "IMPORT"}),
            @JsonSubTypes.Type(value = DatabaseChangeParameters.class, names = {"ASYNC"}),
            @JsonSubTypes.Type(value = PartitionPlanTaskParameters.class, name = "PARTITION_PLAN"),
            @JsonSubTypes.Type(value = ShadowTableSyncTaskParameter.class, name = "SHADOWTABLE_SYNC"),
            @JsonSubTypes.Type(value = AlterScheduleParameters.class, name = "ALTER_SCHEDULE"),
            @JsonSubTypes.Type(value = OnlineSchemaChangeParameters.class, name = "ONLINE_SCHEMA_CHANGE"),
            @JsonSubTypes.Type(value = ResultSetExportTaskParameter.class, name = "EXPORT_RESULT_SET"),
            @JsonSubTypes.Type(value = ApplyProjectParameter.class, name = "APPLY_PROJECT_PERMISSION")
    })
    private TaskParameters parameters;

    /**
     * Followings are filled by aspect {@link CreateFlowInstanceProcessAspect}
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Long projectId;
    @JsonProperty(access = Access.READ_ONLY)
    private String projectName;
    @JsonProperty(access = Access.READ_ONLY)
    private String databaseName;
    @JsonProperty(access = Access.READ_ONLY)
    private Long connectionId;
    @JsonProperty(access = Access.READ_ONLY)
    private String connectionName;
    @JsonProperty(access = Access.READ_ONLY)
    private Long environmentId;
    @JsonProperty(access = Access.READ_ONLY)
    private String environmentName;

    public void validate() {
        if (executionStrategy == FlowTaskExecutionStrategy.TIMER) {
            PreConditions.notNull(executionTime, "executionTime");
        }
    }

}
