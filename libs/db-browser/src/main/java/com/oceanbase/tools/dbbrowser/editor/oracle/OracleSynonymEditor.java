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
package com.oceanbase.tools.dbbrowser.editor.oracle;

import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.editor.DBObjectEditor;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * {@link OracleSynonymEditor}
 *
 * @author yh263208
 * @date 2023-02-23 15:53
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OracleSynonymEditor implements DBObjectEditor<DBSynonym> {

    @Override
    public boolean editable() {
        return false;
    }

    @Override
    public String generateCreateObjectDDL(@NotNull DBSynonym dbObject) {
        Validate.notBlank(dbObject.getSynonymName(), "Synonym name can not be blank");
        Validate.notBlank(dbObject.getTableName(), "Table name can not be blank");
        Validate.notNull(dbObject.getSynonymType(), "Synonym type can not be null");
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("CREATE OR REPLACE ");
        if (dbObject.getSynonymType() == DBSynonymType.PUBLIC) {
            sqlBuilder.append("PUBLIC ");
        }
        sqlBuilder.append("SYNONYM ")
                .identifier(dbObject.getSynonymName())
                .append(" FOR ");
        if (StringUtils.isNotBlank(dbObject.getTableOwner())) {
            sqlBuilder.identifier(dbObject.getTableOwner())
                    .append(".");
        }
        return sqlBuilder.identifier(dbObject.getTableName()).append(";").toString();
    }

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBSynonym dbObject) {
        return generateCreateObjectDDL(dbObject);
    }

    @Override
    public String generateUpdateObjectDDL(@NotNull DBSynonym oldObject,
            @NotNull DBSynonym newObject) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBSynonym> oldObjects,
            Collection<DBSynonym> newObjects) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBSynonym oldObject,
            @NotNull DBSynonym newObject) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBSynonym dbObject) {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
