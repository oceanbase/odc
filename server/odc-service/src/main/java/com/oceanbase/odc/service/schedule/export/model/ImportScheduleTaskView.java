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

import javax.annotation.Nullable;

import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.Data;

@Data
public class ImportScheduleTaskView {

    /**
     * The unique ID of the exported file, which uniquely represents one schedule in one exported file
     */
    private String exportRowId;

    /**
     * Indicates whether a schedule can be imported
     */
    private Boolean importable;

    /**
     * Reasons for not being importable
     */
    @Nullable
    private ScheduleNonImportableType nonImportableType;

    /**
     * Schedule id of the system before export
     */
    private String originId;

    /**
     * Project name of the system before export
     */
    @Nullable
    private String originProjectName;

    private ScheduleType type;


    private ImportDatabaseView databaseView;
    @Nullable

    private ImportDatabaseView targetDatabaseView;
}
