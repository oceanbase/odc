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
package com.oceanbase.tools.dbbrowser.editor;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.model.DBMViewLogPurgeSchedule;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import jakarta.validation.constraints.NotNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/7/16 19:31
 * @since: 4.4.0
 */
public abstract class DBMViewLogEditor implements DBObjectEditor<DBMaterializedViewLog> {

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public String generateCreateObjectDDL(DBMaterializedViewLog dbObject) {
        throw new NotImplementedException();
    }

    @Override
    public String generateCreateDefinitionDDL(DBMaterializedViewLog dbObject) {
        throw new NotImplementedException();
    }

    @Override
    public String generateUpdateObjectDDL(DBMaterializedViewLog oldObject, DBMaterializedViewLog newObject) {
        if (Objects.equals(oldObject.getPurgeParallelismDegree(), newObject.getPurgeParallelismDegree())
                && Objects.equals(oldObject.getPurgeSchedule(), newObject.getPurgeSchedule())) {
            return "";
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        boolean isFirstAction = true;
        sqlBuilder.append("ALTER MATERIALIZED VIEW LOG ON ").append(getFullyQualifiedTableName(newObject));
        if (!Objects.equals(oldObject.getPurgeParallelismDegree(), newObject.getPurgeParallelismDegree())) {
            isFirstAction = false;
            sqlBuilder.line().append("PARALLEL ").append(newObject.getPurgeParallelismDegree());
        }
        if (null != newObject.getPurgeSchedule()
                && !Objects.equals(oldObject.getPurgeSchedule(), newObject.getPurgeSchedule())) {
            if (!isFirstAction) {
                sqlBuilder.append(",");
            }
            DBMViewLogPurgeSchedule newPurgeSchedule = newObject.getPurgeSchedule();
            fillUpdatePurgeScheduleDDL(sqlBuilder, newPurgeSchedule);
        }
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBMaterializedViewLog> oldObjects,
            Collection<DBMaterializedViewLog> newObjects) {
        throw new NotImplementedException();
    }

    @Override
    public String generateRenameObjectDDL(DBMaterializedViewLog oldObject, DBMaterializedViewLog newObject) {
        throw new NotImplementedException();
    }

    @Override
    public String generateDropObjectDDL(DBMaterializedViewLog dbObject) {
        throw new NotImplementedException();
    }

    protected abstract SqlBuilder sqlBuilder();

    protected abstract void fillUpdatePurgeScheduleDDL(@NotNull SqlBuilder sqlBuilder,
            @NotNull DBMViewLogPurgeSchedule newPurgeSchedule);

    private String getFullyQualifiedTableName(@NotNull DBMaterializedViewLog mViewLog) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(mViewLog.getSchemaName())) {
            sqlBuilder.identifier(mViewLog.getSchemaName()).append(".");
        }
        sqlBuilder.identifier(mViewLog.getBaseTableName());
        return sqlBuilder.toString();
    }

}
