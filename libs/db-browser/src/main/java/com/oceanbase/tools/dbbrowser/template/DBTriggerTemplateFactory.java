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
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleTriggerTemplate;

public class DBTriggerTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBTrigger>> {

    @Override
    public DBObjectTemplate<DBTrigger> buildForDoris() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBTrigger> buildForMySQL() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBTrigger> buildForOBMySQL() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBTrigger> buildForOBOracle() {
        return new OracleTriggerTemplate();
    }

    @Override
    public DBObjectTemplate<DBTrigger> buildForOracle() {
        return new OracleTriggerTemplate();
    }

    @Override
    public DBObjectTemplate<DBTrigger> buildForOdpSharding() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBObjectTemplate<DBTrigger> buildForPostgres() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
