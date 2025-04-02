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
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.template.BaseMViewTemplate;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.Getter;
import lombok.Setter;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/4/1 21:45
 * @since: 4.3.4
 */
public abstract class DBMViewEditor implements DBObjectEditor<DBMaterializedView> {
    // @Getter
    // @Setter
    // protected DBObjectEditor<DBTable> tableEditor;
    @Getter
    @Setter
    protected DBObjectEditor<DBTableIndex> indexEditor;

    public DBMViewEditor(DBObjectEditor<DBTableIndex> indexEditor) {
        this.indexEditor = indexEditor;
    }

    /**
     * {@link DBObjectType#MATERIALIZED_VIEW} cannot directly generate a complete creation statement,
     * but can only generate a template to be perfected by the user. For details about how to generate
     * templates, see {@link BaseMViewTemplate#generateCreateObjectTemplate(DBMaterializedView)}
     *
     * @param materializedView {@link DBMaterializedView}
     * @return
     */
    @Override
    public String generateCreateObjectDDL(@NotNull DBMaterializedView materializedView) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBMaterializedView table) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBMaterializedView table) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("DROP MATERIALIZED VIEW ").append(getFullyQualifiedTableName(table));
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectDDL(@NotNull DBMaterializedView oldTable,
            @NotNull DBMaterializedView newTable) {
        SqlBuilder sqlBuilder = sqlBuilder();
        fillIndexSchemaNameAndTableName(oldTable.getIndexes(), oldTable.getSchemaName(), oldTable.getName());
        fillIndexSchemaNameAndTableName(newTable.getIndexes(), newTable.getSchemaName(), newTable.getName());
        sqlBuilder.append(indexEditor.generateUpdateObjectListDDL(oldTable.getIndexes(), newTable.getIndexes()));
        return sqlBuilder.toString();
    }

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBMaterializedView> oldObjects,
            Collection<DBMaterializedView> newObjects) {
        throw new NotImplementedException();
    }

    @Override
    public String generateRenameObjectDDL(DBMaterializedView oldObject, DBMaterializedView newObject) {
        throw new UnsupportedOperationException("not supported");
    }

    protected abstract SqlBuilder sqlBuilder();

    protected String getFullyQualifiedTableName(@NotNull DBMaterializedView table) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(table.getSchemaName())) {
            sqlBuilder.identifier(table.getSchemaName()).append(".");
        }
        sqlBuilder.identifier(table.getName());
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
