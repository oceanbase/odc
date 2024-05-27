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
package com.oceanbase.tools.dbbrowser.factory.schema;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactories;
import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactory;
import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactoryConfig;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBSchemaAccessorFactories implements DBBrowserFactories<DBSchemaAccessor> {

    private String dbVersion;
    private DataSource dataSource;
    private Properties properties;

    @Override
    public DBBrowserFactory<DBSchemaAccessor> buildForMysql() {
        return null;
    }

    @Override
    public DBBrowserFactory<DBSchemaAccessor> buildForOBMysql() {
        return new OBMySQLDBSchemaAccessorFactory(new JdbcTemplate(this.dataSource), this.dbVersion,
                this.properties.getProperty(DBBrowserFactoryConfig.TENANTNAME_KEY));
    }

    @Override
    public DBBrowserFactory<DBSchemaAccessor> buildForOBOracle() {
        return new OBOracleDBSchemaAccessorFactory(new JdbcTemplate(this.dataSource), this.dbVersion);
    }

    @Override
    public DBBrowserFactory<DBSchemaAccessor> buildForOracle() {
        return null;
    }

    @Override
    public DBBrowserFactory<DBSchemaAccessor> buildForDoris() {
        return null;
    }

    @Override
    public DBBrowserFactory<DBSchemaAccessor> build(DBBrowserFactoryConfig config) {
        this.dbVersion = config.getDbVersion();
        this.dataSource = config.getDataSource();
        this.properties = config.getProperties();
        return build(config.getType());
    }

}
