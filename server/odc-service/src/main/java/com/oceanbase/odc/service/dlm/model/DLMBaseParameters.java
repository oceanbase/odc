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

import java.util.List;

import com.oceanbase.odc.service.schedule.model.ScheduleTaskParameters;
import com.oceanbase.tools.migrator.common.configure.DirtyRowAction;
import com.oceanbase.tools.migrator.common.enums.ShardingStrategy;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2025/2/17 10:30
 * @Descripition:
 */

@Data
public class DLMBaseParameters implements ScheduleTaskParameters {

    private List<OffsetConfig> variables;

    private List<DataArchiveTableConfig> tables;

    private boolean needPrintSqlTrace = false;

    private int readThreadCount;

    private int writeThreadCount;

    private int queryTimeout;

    private int scanBatchSize;

    private Long timeoutMillis;

    private RateLimitConfiguration rateLimit;

    private ShardingStrategy shardingStrategy;

    private boolean fullDatabase = false;

    private DirtyRowAction dirtyRowAction = DirtyRowAction.RAISE_ERROR;

    private Long maxAllowedDirtyRowCount;

    private boolean deleteTemporaryTable = false;

}
