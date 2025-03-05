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
package com.oceanbase.odc.service.schedule.archiverist.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.Data;

@Data
public class SqlPlanScheduleRowData extends BaseScheduleRowData {

    private ExportedDatabase targetDatabase;

    private String sqlContent;
    // 用于前端展示执行SQL文件名
    private List<String> sqlObjectNames;
    // 用于从文件目录里面获取File对象
    private List<String> sqlObjectIds;
    // 用于前端展示回滚SQL文件名
    private List<String> rollbackSqlObjectIds;
    private String rollbackSqlContent;
    // 用于从文件目录里面获取File对象
    private List<String> rollbackSqlFileNames;

    private Long timeoutMillis = 172800000L;// 2d for default
    private TaskErrorStrategy errorStrategy;
    private boolean markAsFailedWhenAnyErrorsHappened;
    private String delimiter = ";";
    private Integer queryLimit = 1000;
    private Integer riskLevelIndex;
    @NotNull
    private Boolean generateRollbackPlan;
    private boolean modifyTimeoutIfTimeConsumingSqlExists = true;
    // internal usage for notification
    private ScheduleType parentScheduleType;
    private Integer retryTimes = 0;
    private Long retryIntervalMillis = 180000L;

}
