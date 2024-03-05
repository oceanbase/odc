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
 *
 * {@link MySQLNoLessThan5600SchemaAccessor}
 *
 * 适配 MySQL 版本：[5.6.00, 5.7.00)
 *
 * @author jingtian
 * @date 2024/2/26
 * @since ODC_release_4.2.4
 */
public class MySQLNoLessThan5600SchemaAccessor extends MySQLNoLessThan5700SchemaAccessor {
    public MySQLNoLessThan5600SchemaAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.MYSQL_5_6_x);
    }

    @Override
    protected boolean supportGeneratedColumn() {
        return false;
    }
}
