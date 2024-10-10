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
package com.oceanbase.odc.service.schedule.model;

import java.util.Date;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.loaddata.model.LoadDataParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/6/18 10:55
 * @Descripition:
 */

@Data
public class ScheduleTask {

    private Long id;
    @NotNull
    private String jobName;
    @NotNull
    private String jobGroup;
    @NotNull
    @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "jobGroup")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = DataArchiveParameters.class, name = "DATA_ARCHIVE"),
            @JsonSubTypes.Type(value = DataDeleteParameters.class, name = "DATA_DELETE"),
            @JsonSubTypes.Type(value = DataArchiveClearParameters.class, names = "DATA_ARCHIVE_DELETE"),
            @JsonSubTypes.Type(value = DataArchiveRollbackParameters.class, names = "DATA_ARCHIVE_ROLLBACK"),
            @JsonSubTypes.Type(value = LoadDataParameters.class, name = "LOAD_DATA"),
            @JsonSubTypes.Type(value = SqlPlanParameters.class, name = "SQL_PLAN"),
            @JsonSubTypes.Type(value = OnlineSchemaChangeParameters.class, name = "ONLINE_SCHEMA_CHANGE_COMPLETE"),
            @JsonSubTypes.Type(value = LogicalDatabaseChangeParameters.class, name = "LOGICAL_DATABASE_CHANGE")
    })
    private ScheduleTaskParameters parameters;

    private TaskStatus status;

    private Date fireTime;

    private double progressPercentage;

    private String resultJson;

    private String executor;

    private Date createTime;

    private Date updateTime;

    private Long jobId;

}
