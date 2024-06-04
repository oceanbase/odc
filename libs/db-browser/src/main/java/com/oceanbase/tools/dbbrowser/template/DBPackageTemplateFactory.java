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
package com.oceanbase.tools.dbbrowser.template;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.template.oracle.OraclePackageTemplate;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBPackageTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBPackage>> {

    private DataSource dataSource;
    private JdbcOperations jdbcOperations;

    @Override
    public DBObjectTemplate<DBPackage> buildForDoris() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBPackage> buildForMySQL() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBPackage> buildForOBMySQL() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBPackage> buildForOBOracle() {
        return new OraclePackageTemplate(getJdbcOperations());
    }

    @Override
    public DBObjectTemplate<DBPackage> buildForOracle() {
        return new OraclePackageTemplate(getJdbcOperations());
    }

    @Override
    public DBObjectTemplate<DBPackage> buildForOdpSharding() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    private JdbcOperations getJdbcOperations() {
        if (this.jdbcOperations != null) {
            return this.jdbcOperations;
        } else if (this.dataSource != null) {
            return new JdbcTemplate(this.dataSource);
        }
        throw new IllegalArgumentException("Datasource can not be null");
    }

}
