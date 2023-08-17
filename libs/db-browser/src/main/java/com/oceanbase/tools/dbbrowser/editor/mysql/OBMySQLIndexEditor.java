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

import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2022/7/19 下午5:04
 * @Description: []
 */
public class OBMySQLIndexEditor extends MySQLNoGreaterThan5740IndexEditor {


    @Override
    protected void appendIndexModifiers(DBTableIndex index, SqlBuilder sqlBuilder) {
        if (DBIndexType.FULLTEXT == index.getType()) {
            sqlBuilder.append(" FULLTEXT");
        } else {
            /**
             * OB MySQL 除 FULLTEXT 之外的类型，只有 UNIQUE 和 非 UNIQUE； 如果是非 UNIQUE，则不需要加 modifiers; 如果是
             * UNIQUE，则调用父类方法，添加 UNIQUE 关键字
             */
            super.appendIndexModifiers(index, sqlBuilder);
        }
    }

    @Override
    protected void appendIndexOptions(DBTableIndex index, SqlBuilder sqlBuilder) {
        if (Objects.nonNull(index.getGlobal())) {
            sqlBuilder.append(index.getGlobal() ? " GLOBAL " : " LOCAL ");
        }
        if (Objects.nonNull(index.getKeyBlockSize())) {
            sqlBuilder.append(" KEY_BLOCK_SIZE ").append(String.valueOf(index.getKeyBlockSize()));
        }
        if (index.getType() == DBIndexType.FULLTEXT && StringUtils.isNotBlank(index.getParserName())) {
            sqlBuilder.append(" WITH PARSER ").append(index.getParserName());
        }
        if (StringUtils.isNotBlank(index.getComment())) {
            sqlBuilder.append(" COMMENT ").value(index.getComment());
        }
        if (Objects.nonNull(index.getVisible()) && !index.getVisible()) {
            sqlBuilder.append(" INVISIBLE ");
        }
    }

}
