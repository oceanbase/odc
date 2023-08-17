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
import com.oceanbase.tools.dbbrowser.model.DBSequence;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * {@link OracleSequenceEditor}
 *
 * @author yh263208
 * @date 2023-02-22 19:20
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OracleSequenceEditor implements DBObjectEditor<DBSequence> {

    @Override
    public boolean editable() {
        return false;
    }

    @Override
    public String generateCreateObjectDDL(@NotNull DBSequence dbObject) {
        Validate.notBlank(dbObject.getName(), "Sequence name can not be blank");
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("CREATE SEQUENCE ").identifier(dbObject.getName());
        if (dbObject.getMinValue() != null) {
            sqlBuilder.append(" MINVALUE ").append(dbObject.getMinValue());
        } else {
            sqlBuilder.append(" NOMINVALUE");
        }
        if (dbObject.getMaxValue() != null) {
            sqlBuilder.append(" MAXVALUE ").append(dbObject.getMaxValue());
        } else {
            sqlBuilder.append(" NOMAXVALUE");
        }
        if (dbObject.getStartValue() != null) {
            sqlBuilder.append(" START WITH ").append(dbObject.getStartValue());
        }
        if (dbObject.getIncreament() != null) {
            sqlBuilder.append(" INCREMENT BY ").append(dbObject.getIncreament());
        }
        if (dbObject.getCached() != null) {
            if (dbObject.getCached() && dbObject.getCacheSize() != null) {
                sqlBuilder.append(" CACHE ").append(dbObject.getCacheSize());
            } else {
                sqlBuilder.append(" NOCACHE");
            }
        }
        if (dbObject.getOrderd() != null) {
            if (dbObject.getOrderd()) {
                sqlBuilder.append(" ORDER");
            } else {
                sqlBuilder.append(" NOORDER");
            }
        }
        if (dbObject.getCycled() != null) {
            if (dbObject.getCycled()) {
                sqlBuilder.append(" CYCLE");
            } else {
                sqlBuilder.append(" NOCYCLE");
            }
        }
        return sqlBuilder.append(";").toString();
    }

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBSequence dbObject) {
        return generateCreateObjectDDL(dbObject);
    }

    @Override
    public String generateUpdateObjectDDL(@NotNull DBSequence oldObject, @NotNull DBSequence newObject) {
        Validate.notBlank(oldObject.getName(), "Sequence name can not be blank");
        Validate.isTrue(StringUtils.equals(oldObject.getName(), newObject.getName()));
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("ALTER SEQUENCE ").identifier(oldObject.getName());
        if (newObject.getMinValue() != null) {
            sqlBuilder.append(" MINVALUE ").append(newObject.getMinValue());
        } else {
            sqlBuilder.append(" NOMINVALUE");
        }
        if (newObject.getMaxValue() != null) {
            sqlBuilder.append(" MAXVALUE ").append(newObject.getMaxValue());
        } else {
            sqlBuilder.append(" NOMAXVALUE");
        }
        if (newObject.getIncreament() != null) {
            sqlBuilder.append(" INCREMENT BY ").append(newObject.getIncreament());
        }
        if (newObject.getCached() != null) {
            if (newObject.getCached() && newObject.getCacheSize() != null) {
                sqlBuilder.append(" CACHE ").append(newObject.getCacheSize());
            } else {
                sqlBuilder.append(" NOCACHE");
            }
        }
        if (newObject.getOrderd() != null) {
            if (newObject.getOrderd()) {
                sqlBuilder.append(" ORDER");
            } else {
                sqlBuilder.append(" NOORDER");
            }
        }
        if (newObject.getCycled() != null) {
            if (newObject.getCycled()) {
                sqlBuilder.append(" CYCLE");
            } else {
                sqlBuilder.append(" NOCYCLE");
            }
        }
        return sqlBuilder.append(";").toString();
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBSequence> oldObjects,
            Collection<DBSequence> newObjects) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBSequence oldObject,
            @NotNull DBSequence newObject) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBSequence dbObject) {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
