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
package com.oceanbase.tools.dbbrowser.schema.mysql;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;

/**
 * {@link OBMySQLBetween220And225XSchemaAccessor}
 * 
 * 适配 OB 版本：(1.4.79, 2.2.60)
 * 
 * @author yh263208
 * @date 2023-03-03 14:52
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OBMySQLBetween220And225XSchemaAccessor extends BaseOBMySQLLessThan2277SchemaAccessor {

    public OBMySQLBetween220And225XSchemaAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations, DBSchemaAccessorSqlMappers.get(StatementsFiles.OBMYSQL_225X));
    }
}
