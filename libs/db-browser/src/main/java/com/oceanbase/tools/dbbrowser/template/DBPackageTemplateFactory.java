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

import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.template.oracle.OraclePackageTemplate;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBPackageTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBPackage>> {

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
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        return new OraclePackageTemplate(this.jdbcOperations);
    }

    @Override
    public DBObjectTemplate<DBPackage> buildForOracle() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        return new OraclePackageTemplate(this.jdbcOperations);
    }

    @Override
    public DBObjectTemplate<DBPackage> buildForOdpSharding() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
