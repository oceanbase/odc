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
package com.oceanbase.odc.service.databasechange.model;

import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class DatabaseChangeDatabase {

    private static final long serialVersionUID = -5013749085190365604L;
    private Long id;
    private String databaseId;
    private Boolean existed;
    private String name;
    private DatabaseChangeProject project;
    private DatabaseChangeConnection dataSource;
    private DatabaseChangeEnvironment environment;

    public DatabaseChangeDatabase(@NonNull Database database) {
        this.id = database.getId();
        this.databaseId = database.getDatabaseId();
        this.existed = database.getExisted();
        this.name = database.getName();
        this.project = new DatabaseChangeProject(database.getProject());
        this.dataSource = new DatabaseChangeConnection(database.getDataSource());
        this.environment = new DatabaseChangeEnvironment(database.getEnvironment());
    }

    public DatabaseChangeDatabase(@NonNull Long id) {
        this.id = id;
    }

}
