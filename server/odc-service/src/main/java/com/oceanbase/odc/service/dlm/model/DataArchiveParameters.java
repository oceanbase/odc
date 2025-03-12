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

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.migrator.common.configure.DirtyRowAction;
import com.oceanbase.tools.migrator.common.enums.MigrationInsertAction;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2023/5/10 20:05
 * @Descripition:
 */
@Data
public class DataArchiveParameters extends DLMBaseParameters {

    @NotNull
    private Long sourceDatabaseId;

    @NotNull
    private Long targetDataBaseId;

    @JsonProperty(access = Access.READ_ONLY)
    private Database sourceDatabase;

    @JsonProperty(access = Access.READ_ONLY)
    private Database targetDatabase;

    private boolean deleteAfterMigration = false;

    private boolean deleteTemporaryTable = false;

    private Set<DBObjectType> syncTableStructure = new HashSet<>();

    private MigrationInsertAction migrationInsertAction = MigrationInsertAction.INSERT_NORMAL;

    private boolean fullDatabase = false;

}
