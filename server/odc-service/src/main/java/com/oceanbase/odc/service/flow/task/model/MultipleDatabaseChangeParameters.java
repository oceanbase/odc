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
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeDatabase;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeProject;

import lombok.Data;

/**
 * @author: zijia.cj
 * @date: 2024/3/27
 */
@Data
public class MultipleDatabaseChangeParameters extends DatabaseChangeParameters {
    /**
     * Because ids in a Project cannot be deserialized, this property is needed instead of receiving
     * front-end data
     */
    private Long projectId;
    /**
     * All databases must belong to this project
     */
    private DatabaseChangeProject project;
    /**
     * multiple databases change execution sequence
     */
    private List<List<Long>> orderedDatabaseIds;
    private List<DatabaseChangeDatabase> databases;
    private Integer batchId;
    /**
     * Error strategy in multiple databases auto execution mode
     */
    private TaskErrorStrategy autoErrorStrategy;
    /**
     * TimeoutMillis in multiple databases manual execution mode
     */
    private Long manualTimeoutMillis = 1000 * 60 * 60 * 24 * 2L;// 2d for default

    public DatabaseChangeParameters convertIntoDatabaseChangeParameters(MultipleDatabaseChangeParameters parameter) {
        DatabaseChangeParameters databaseChangeParameters = new DatabaseChangeParameters();
        databaseChangeParameters.setSqlContent(parameter.getSqlContent());
        databaseChangeParameters.setSqlObjectNames(parameter.getSqlObjectNames());
        databaseChangeParameters.setSqlObjectIds(parameter.getSqlObjectIds());
        databaseChangeParameters
                .setRollbackSqlObjectNames(parameter.getRollbackSqlObjectNames());
        databaseChangeParameters.setRollbackSqlContent(parameter.getRollbackSqlContent());
        databaseChangeParameters.setRollbackSqlObjectIds(parameter.getRollbackSqlObjectIds());
        // Error strategy for sql changes in a single database
        databaseChangeParameters.setErrorStrategy(parameter.getErrorStrategy().toString());
        databaseChangeParameters.setDelimiter(parameter.getDelimiter());
        databaseChangeParameters.setGenerateRollbackPlan(parameter.getGenerateRollbackPlan());
        return databaseChangeParameters;
    }

}
