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

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLFunctionTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleFunctionTemplate;

public class DBFunctionTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBFunction>> {

    @Override
    public DBObjectTemplate<DBFunction> buildForDoris() {
        return new MySQLFunctionTemplate();
    }

    @Override
    public DBObjectTemplate<DBFunction> buildForMySQL() {
        return new MySQLFunctionTemplate();
    }

    @Override
    public DBObjectTemplate<DBFunction> buildForOBMySQL() {
        return new MySQLFunctionTemplate();
    }

    @Override
    public DBObjectTemplate<DBFunction> buildForOBOracle() {
        return new OracleFunctionTemplate();
    }

    @Override
    public DBObjectTemplate<DBFunction> buildForOracle() {
        return new OracleFunctionTemplate();
    }

    @Override
    public DBObjectTemplate<DBFunction> buildForOdpSharding() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBFunction> buildForPostgres() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
