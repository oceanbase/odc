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
package com.oceanbase.odc.service.dlm.model;

import java.util.Date;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.schedule.model.DlmTableUnitStatistic;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.limiter.LimiterConfig;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2023/5/31 15:01
 * @Descripition: This entity represents a DLM task unit which can be mapped to a HistoryJob.
 * @see com.oceanbase.tools.migrator.common.dto.HistoryJob
 */

@Data
public class DlmTableUnit {

    private String dlmTableUnitId;

    private Long scheduleTaskId;

    private String tableName;

    private String targetTableName;

    private Date fireTime;

    private DataSourceInfo sourceDatasourceInfo;

    private DataSourceInfo targetDatasourceInfo;

    private LimiterConfig sourceLimitConfig;

    private LimiterConfig targetLimitConfig;

    private DlmTableUnitStatistic statistic;

    private DlmTableUnitParameters parameters;

    private TaskStatus status;

    private JobType type;

    private Date startTime;

    private Date endTime;

    private Set<DBObjectType> syncTableStructure;

}
