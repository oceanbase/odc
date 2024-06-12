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
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLProcedureTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleProcedureTemplate;

public class DBProcedureTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBProcedure>> {

    @Override
    public DBObjectTemplate<DBProcedure> buildForDoris() {
        return new MySQLProcedureTemplate();
    }

    @Override
    public DBObjectTemplate<DBProcedure> buildForMySQL() {
        return new MySQLProcedureTemplate();
    }

    @Override
    public DBObjectTemplate<DBProcedure> buildForOBMySQL() {
        return new MySQLProcedureTemplate();
    }

    @Override
    public DBObjectTemplate<DBProcedure> buildForOBOracle() {
        return new OracleProcedureTemplate();
    }

    @Override
    public DBObjectTemplate<DBProcedure> buildForOracle() {
        return new OracleProcedureTemplate();
    }

    @Override
    public DBObjectTemplate<DBProcedure> buildForOdpSharding() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
