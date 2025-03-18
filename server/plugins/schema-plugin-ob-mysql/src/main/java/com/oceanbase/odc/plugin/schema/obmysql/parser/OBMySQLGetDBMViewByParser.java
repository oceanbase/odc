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
package com.oceanbase.odc.plugin.schema.obmysql.parser;

import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.CreateMaterializedView;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/18 22:58
 * @since: 4.3.4
 */
@Slf4j
public class OBMySQLGetDBMViewByParser extends OBMySQLGetDBTableByParser {

    private final CreateMaterializedView createMaterializedView;

    public OBMySQLGetDBMViewByParser(@NonNull String tableDDL) {
        super(tableDDL);
        this.createMaterializedView = parseTableDDL(tableDDL);
    }

    private CreateMaterializedView parseTableDDL(String ddl) {
        CreateMaterializedView statement = null;
        try {
            Statement value = SqlParser.parseMysqlStatement(ddl);
            if (value instanceof CreateMaterializedView) {
                statement = (CreateMaterializedView) value;
            }
        } catch (Exception e) {
            log.warn("Failed to parse table ddl, error message={}", e.getMessage());
        }
        return statement;
    }

    @Override
    protected CreateTable getCreateTableStmt() {
        return null;
    }

    @Override
    protected CreateMaterializedView getCreateMaterializedViewStmt() {
        return this.createMaterializedView;
    }

}
