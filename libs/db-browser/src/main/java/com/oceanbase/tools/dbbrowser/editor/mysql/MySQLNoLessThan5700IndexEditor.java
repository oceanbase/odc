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
package com.oceanbase.tools.dbbrowser.editor.mysql;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * 适配 MySQL 版本：[5.7.00, ~)
 * 
 * @author jingtian
 */
public class MySQLNoLessThan5700IndexEditor extends DBTableIndexEditor {

    @Override
    protected void appendIndexColumnModifiers(DBTableIndex index, SqlBuilder sqlBuilder) {}

    @Override
    public String generateRenameObjectDDL(@NotNull DBTableIndex oldIndex, @NotNull DBTableIndex newIndex) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(oldIndex))
                .append(" RENAME INDEX ").identifier(oldIndex.getName()).append(" TO ").identifier(newIndex.getName());
        return sqlBuilder.toString();
    }

    @Override
    protected void appendIndexModifiers(DBTableIndex index, SqlBuilder sqlBuilder) {
        if (DBIndexType.FULLTEXT == index.getType()) {
            sqlBuilder.append(" FULLTEXT");
        } else if (DBIndexType.SPATIAL == index.getType()) {
            sqlBuilder.append(" SPATIAL");
        } else {
            /**
             * MySQL 除 FULLTEXT 和 SPATIAL 之外的类型，只有 UNIQUE 和 非 UNIQUE； 如果是非 UNIQUE，则不需要加 modifiers; 如果是
             * UNIQUE，则调用父类方法，添加 UNIQUE 关键字
             */
            super.appendIndexModifiers(index, sqlBuilder);
        }
    }

    @Override
    protected void appendIndexType(DBTableIndex index, SqlBuilder sqlBuilder) {
        /**
         * MySQL 没有 USING FULLTEXT 的写法
         */
        DBIndexAlgorithm algorithm = index.getAlgorithm();
        if (DBIndexType.FULLTEXT != index.getType() && Objects.nonNull(algorithm)
                && algorithm != DBIndexAlgorithm.UNKNOWN) {
            sqlBuilder.append(" USING ").append(index.getAlgorithm().getValue());
        }
    }

    @Override
    protected void appendIndexOptions(DBTableIndex index, SqlBuilder sqlBuilder) {
        if (Objects.nonNull(index.getKeyBlockSize())) {
            sqlBuilder.append(" KEY_BLOCK_SIZE ").append(String.valueOf(index.getKeyBlockSize()));
        }
        if (index.getType() == DBIndexType.FULLTEXT && StringUtils.isNotBlank(index.getParserName())) {
            sqlBuilder.append(" WITH PARSER ").append(index.getParserName());
        }
        if (StringUtils.isNotBlank(index.getComment())) {
            sqlBuilder.append(" COMMENT ").value(index.getComment());
        }
    }

    @Override
    protected SqlBuilder sqlBuilder() {
        return new MySQLSqlBuilder();
    }

    @Override
    public boolean editable() {
        return true;
    }

}
