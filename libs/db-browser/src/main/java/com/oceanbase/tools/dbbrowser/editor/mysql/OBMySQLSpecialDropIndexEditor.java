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

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @description: {@link OBMySQLSpecialDropIndexEditor#generateDropObjectDDL(DBTableIndex)} is used to generate the 'DROP INDEX `indexName` ON `databaseName`.`tableName`' statement.
 * The existing {@link OBMySQLIndexEditor#generateDropObjectDDL(DBTableIndex)} is used to generate 'ALTER `databaseName`.`tableName` DROP `indexName`' statements,which does not support {@link DBObjectType#MATERIALIZED_VIEW} in ob mysql tenant.
 *               So add the {@link OBMySQLSpecialDropIndexEditor} to solve that problem.
 * @author: zijia.cj
 * @date: 2025/4/2 11:04
 * @since: 4.3.4
 */
public class OBMySQLSpecialDropIndexEditor extends OBMySQLIndexEditor {
    @Override
    public String generateDropObjectDDL(@NotNull DBTableIndex dbObject) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("DROP INDEX ").identifier(dbObject.getName()).append(" ON ")
                .append(getFullyQualifiedTableName(dbObject));
        return sqlBuilder.toString().trim() + ";\n";
    }

}
