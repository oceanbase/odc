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

import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.OffsetConfig;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.tools.migrator.common.enums.ShardingStrategy;

import lombok.Data;

@Data
public class DataDeleteScheduleRowData extends BaseScheduleRowData {

    @NotNull
    private ArchiveDatabase database;

    private ArchiveDatabase targetDatabase;

    private List<OffsetConfig> variables;

    private List<DataArchiveTableConfig> tables;

    @NotNull
    private RateLimitConfiguration rateLimit;

    private Boolean deleteByUniqueKey = true;

    private ShardingStrategy shardingStrategy;

    private Boolean needCheckBeforeDelete = false;

    private boolean needPrintSqlTrace = false;

    private int readThreadCount;

    private int writeThreadCount;

    private int queryTimeout;

    private int scanBatchSize;

    private Long timeoutMillis;

    private boolean fullDatabase = false;

    @Override
    public void encrypt(String encryptKey) {
        if (database != null) {
            database.encrypt(encryptKey);
        }
        if (targetDatabase != null) {
            targetDatabase.encrypt(encryptKey);
        }
    }

    @Override
    public void decrypt(String encryptKey) {
        if (database != null) {
            database.decrypt(encryptKey);
        }
        if (targetDatabase != null) {
            targetDatabase.decrypt(encryptKey);
        }
    }
}
