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
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.template.BaseMViewTemplate;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/4/1 21:45
 * @since: 4.3.4
 */
public abstract class DBMViewEditor implements DBObjectEditor<DBMaterializedView> {

    private final DBObjectEditor<DBTableIndex> indexEditor;

    public DBMViewEditor(@NotNull DBObjectEditor<DBTableIndex> indexEditor) {
        this.indexEditor = indexEditor;
    }

    /**
     * {@link DBObjectType#MATERIALIZED_VIEW} cannot directly generate a complete creation statement
     * like {@link DBObjectType#TABLE}, Instead, {@link DBObjectType#MATERIALIZED_VIEW} provides a
     * creation template like {@link DBObjectType#VIEW}, allowing users to refine the template as
     * needed. For details about how to generate the template, see
     * {@link BaseMViewTemplate#generateCreateObjectTemplate(DBMaterializedView)}
     *
     * @param mView {@link DBMaterializedView}
     * @return
     */
    @Override
    public String generateCreateObjectDDL(@NotNull DBMaterializedView mView) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBMaterializedView mView) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBMaterializedView mView) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("DROP MATERIALIZED VIEW ").append(getFullyQualifiedTableName(mView));
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectDDL(@NotNull DBMaterializedView oldMView,
            @NotNull DBMaterializedView newMView) {
        SqlBuilder sqlBuilder = sqlBuilder();
        fillIndexSchemaNameAndTableName(oldMView.getIndexes(), oldMView.getSchemaName(), oldMView.getName());
        fillIndexSchemaNameAndTableName(newMView.getIndexes(), newMView.getSchemaName(), newMView.getName());
        sqlBuilder.append(indexEditor.generateUpdateObjectListDDL(oldMView.getIndexes(), newMView.getIndexes()));
        return sqlBuilder.toString();
    }

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBMaterializedView> oldObjects,
            Collection<DBMaterializedView> newObjects) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateRenameObjectDDL(DBMaterializedView oldObject, DBMaterializedView newObject) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    protected abstract SqlBuilder sqlBuilder();

    private String getFullyQualifiedTableName(@NotNull DBMaterializedView mView) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(mView.getSchemaName())) {
            sqlBuilder.identifier(mView.getSchemaName()).append(".");
        }
        sqlBuilder.identifier(mView.getName());
        return sqlBuilder.toString();
    }

    private void fillIndexSchemaNameAndTableName(List<DBTableIndex> indexes, String schemaName, String tableName) {
        if (CollectionUtils.isNotEmpty(indexes)) {
            indexes.forEach(index -> {
                index.setSchemaName(schemaName);
                index.setTableName(tableName);
            });
        }
    }

}
