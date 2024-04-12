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

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.schedule.model.JobType;

import lombok.Data;

/**
 * @author: zijia.cj
 * @date: 2024/3/27
 */
@Data
public class MultipleDatabaseChangeParameters implements Serializable, TaskParameters {
    /**
     * 多库执行顺序
     */
    private List<List> orderedDatabaseIds;
    /**
     * 所有数据库集合
     */
    private List<Database> databases;
    /**
     * 当前批次
     */
    private Integer batchId;
    private String sqlContent;
    // 用于前端展示执行SQL文件名
    private List<String> sqlObjectNames;
    private List<String> sqlObjectIds;
    // 用于前端展示回滚SQL文件名
    private List<String> rollbackSqlObjectNames;
    private String rollbackSqlContent;
    private List<String> rollbackSqlObjectIds;
    private Long timeoutMillis = 172800000L;// 2d for default
    private TaskErrorStrategy errorStrategy;
    private String delimiter = ";";
    private Integer queryLimit = 1000;
    private Integer riskLevelIndex;
    @NotNull
    private Boolean generateRollbackPlan;
    private boolean modifyTimeoutIfTimeConsumingSqlExists = true;
    // internal usage for notification
    private JobType parentJobType;

    public void setErrorStrategy(String errorStrategy) {
        this.errorStrategy = TaskErrorStrategy.valueOf(errorStrategy);
    }

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
