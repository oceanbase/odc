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
package com.oceanbase.odc.service.schedule.export.model;

import java.util.Optional;

import javax.annotation.Nullable;

import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.Data;

@Data
public class ScheduleRowPreviewDto {

    private String rowId;
    private String originId;
    private String originProjectName;
    private ScheduleType type;

    private ExportedDatabase database;
    @Nullable
    private ExportedDatabase targetDatabase;

    public ExportedDataSource acquireDatasource() {
        return database.getExportedDataSource();
    }

    public boolean isCloudDatasource() {
        if (targetDatabase != null) {
            String cloudProvider = targetDatabase.getExportedDataSource().getCloudProvider();
            if (cloudProvider != null) {
                return true;
            }
        }
        String cloudProvider = database.getExportedDataSource().getCloudProvider();
        return cloudProvider != null;
    }

    @Nullable
    public ExportedDataSource acquireTargetDatasource() {
        return Optional.ofNullable(targetDatabase).map(ExportedDatabase::getExportedDataSource).orElse(null);
    }

}
