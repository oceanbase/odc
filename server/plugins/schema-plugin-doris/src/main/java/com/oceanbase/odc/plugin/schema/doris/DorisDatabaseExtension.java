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
package com.oceanbase.odc.plugin.schema.doris;

import java.sql.Connection;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.doris.utils.DBAccessorUtil;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLDatabaseExtension;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;

/**
 * ClassName: DorisDatabaseExtension Package: com.oceanbase.odc.plugin.schema.doris Description:
 *
 * @Author: fenghao
 * @Create 2024/1/8 16:44
 * @Version 1.0
 */
@Extension
public class DorisDatabaseExtension extends OBMySQLDatabaseExtension {

    @Override
    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    @Override
    public void create(Connection connection, DBDatabase database, String password) {
        MySQLSqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("create database ").identifier(database.getName());
        // TODO: Support database properties
        JdbcOperationsUtil.getJdbcOperations(connection).execute(sqlBuilder.toString());
    }

}
