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
package com.oceanbase.odc.service.onlineschemachange.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.subtask.SubTaskParameterFactory;

import lombok.Data;

/**
 * Received parameters from user
 *
 * @author yaobin
 * @since 4.2.0
 */
@Data
public class OnlineSchemaChangeParameters implements Serializable, TaskParameters {

    private OnlineSchemaChangeSqlType sqlType;

    private String sqlContent;
    private TaskErrorStrategy errorStrategy;

    private Integer lockTableTimeOutSeconds;

    private Integer swapTableNameRetryTimes;

    private OriginTableCleanStrategy originTableCleanStrategy;

    private String delimiter = ";";

    private List<String> lockUsers;
    private SwapTableType swapTableType;

    public boolean isContinueOnError() {
        return this.errorStrategy == TaskErrorStrategy.CONTINUE;
    }

    public List<OnlineSchemaChangeScheduleTaskParameters> generateSubTaskParameters(ConnectionConfig connectionConfig,
            String schema) {
        List<String> sqls = SqlUtils.split(connectionConfig.getDialectType(), this.sqlContent, this.delimiter);

        try (SubTaskParameterFactory subTaskParameterFactory = new SubTaskParameterFactory(connectionConfig, schema)) {
            Map<TableIdentity, OnlineSchemaChangeScheduleTaskParameters> taskParameters = new LinkedHashMap<>();
            for (String sql : sqls) {
                OnlineSchemaChangeScheduleTaskParameters parameter = subTaskParameterFactory.generate(sql, sqlType);
                TableIdentity key = new TableIdentity(parameter.getDatabaseName(), parameter.getOriginTableName());
                taskParameters.putIfAbsent(key, parameter);
                if (sqlType == OnlineSchemaChangeSqlType.ALTER) {
                    String newAlterStmt = DdlUtils.replaceTableName(sql, parameter.getNewTableName(),
                            connectionConfig.getDialectType(), sqlType);
                    taskParameters.get(key).getSqlsToBeExecuted().add(newAlterStmt);
                }
            }
            return new ArrayList<>(taskParameters.values());
        } catch (Exception e) {
            throw new UnexpectedException("Failed to generate subtasks with parameter: " + this, e);
        }
    }

}
