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
package com.oceanbase.tools.dbbrowser.template;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import jakarta.validation.constraints.NotNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/7/15 19:02
 * @since: 4.4.0
 */
public abstract class BaseMViewLogTemplate implements DBObjectTemplate<DBMaterializedViewLog> {

    @Override
    public String generateCreateObjectTemplate(DBMaterializedViewLog dbMViewLog) {
        Validate.notBlank(dbMViewLog.getBaseTableName(), "Base Table Name can not be blank");
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("CREATE MATERIALIZED VIEW LOG ON ")
                .append(getFullyQualifiedTableName(dbMViewLog));
        if (dbMViewLog.getPurgeParallelismDegree() != null) {
            sqlBuilder.line().append("PARALLEL ").append(dbMViewLog.getPurgeParallelismDegree());
        }
        if (CollectionUtils.isNotEmpty(dbMViewLog.getColumns())) {
            sqlBuilder.line().append("WITH (");
            for (int i = 0; i < dbMViewLog.getColumns().size(); i++) {
                sqlBuilder.identifier(dbMViewLog.getColumns().get(i).getName());
                if (i < dbMViewLog.getColumns().size() - 1) {
                    sqlBuilder.append(",");
                }
            }
            sqlBuilder.append(")");
        }
        if (dbMViewLog.getIncludeNewValues() != null) {
            sqlBuilder.line();
            if (Boolean.TRUE.equals(dbMViewLog.getIncludeNewValues())) {
                sqlBuilder.append("INCLUDING NEW VALUES");
            } else {
                sqlBuilder.append("EXCLUDING NEW VALUES");
            }
        }
        fillPurgeSchedule(dbMViewLog, sqlBuilder);
        return sqlBuilder.toString();
    }

    private String getFullyQualifiedTableName(@NotNull DBMaterializedViewLog mViewLog) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(mViewLog.getSchemaName())) {
            sqlBuilder.identifier(mViewLog.getSchemaName()).append(".");
        }
        sqlBuilder.identifier(mViewLog.getBaseTableName());
        return sqlBuilder.toString();
    }

    protected abstract SqlBuilder sqlBuilder();

    protected abstract void fillPurgeSchedule(DBMaterializedViewLog dbMViewLog, SqlBuilder sqlBuilder);

}
