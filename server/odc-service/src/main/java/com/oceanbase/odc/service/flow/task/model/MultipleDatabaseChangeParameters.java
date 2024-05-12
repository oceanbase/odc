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
package com.oceanbase.odc.service.flow.task.model;

import java.util.List;

import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.Data;

/**
 * @author: zijia.cj
 * @date: 2024/3/27
 */
@Data
public class MultipleDatabaseChangeParameters extends DatabaseChangeParameters {
    /**
     * All databases must belong to this project
     */
    private Long projectId;
    private Project project;
    /**
     * multiple databases change execution sequence
     */
    private List<List<Long>> orderedDatabaseIds;
    private List<Database> databases;
    private Integer           batchId;
    /**
     * Error strategy in multiple databases auto execution mode
     */
    private TaskErrorStrategy autoErrorStrategy;
    /**
     * TimeoutMillis in multiple databases manual execution mode
     */
    private Long manualTimeoutMillis = 1000*60*60*24*2L;// 2d for default

    public DatabaseChangeParameters convertIntoDatabaseChangeParameters(
            MultipleDatabaseChangeParameters multipleDatabaseChangeParameters) {
        DatabaseChangeParameters databaseChangeParameters = new DatabaseChangeParameters();
        databaseChangeParameters.setSqlContent(multipleDatabaseChangeParameters.getSqlContent());
        databaseChangeParameters.setSqlObjectNames(multipleDatabaseChangeParameters.getSqlObjectNames());
        databaseChangeParameters.setSqlObjectIds(multipleDatabaseChangeParameters.getSqlObjectIds());
        databaseChangeParameters
                .setRollbackSqlObjectNames(multipleDatabaseChangeParameters.getRollbackSqlObjectNames());
        databaseChangeParameters.setRollbackSqlContent(multipleDatabaseChangeParameters.getRollbackSqlContent());
        databaseChangeParameters.setRollbackSqlObjectIds(multipleDatabaseChangeParameters.getRollbackSqlObjectIds());
        databaseChangeParameters.setErrorStrategy(multipleDatabaseChangeParameters.getErrorStrategy().toString());
        databaseChangeParameters.setDelimiter(multipleDatabaseChangeParameters.getDelimiter());
        databaseChangeParameters.setGenerateRollbackPlan(multipleDatabaseChangeParameters.getGenerateRollbackPlan());
        databaseChangeParameters.setParentJobType(multipleDatabaseChangeParameters.getParentJobType());
        return databaseChangeParameters;
    }

}
