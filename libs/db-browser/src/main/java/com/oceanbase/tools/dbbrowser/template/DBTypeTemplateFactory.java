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
import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleTypeTemplate;

public class DBTypeTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBType>> {

    @Override
    public DBObjectTemplate<DBType> buildForDoris() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBType> buildForMySQL() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBType> buildForOBMySQL() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBType> buildForOBOracle() {
        return new OracleTypeTemplate();
    }

    @Override
    public DBObjectTemplate<DBType> buildForOracle() {
        return new OracleTypeTemplate();
    }

    @Override
    public DBObjectTemplate<DBType> buildForOdpSharding() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBType> buildForPostgres() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
