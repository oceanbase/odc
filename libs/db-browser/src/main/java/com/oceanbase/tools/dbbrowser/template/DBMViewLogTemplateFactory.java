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
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.template.mysql.OBMySQLMViewLogTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OBOracleMViewLogTemplate;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/7/15 18:59
 * @since: 4.4.0
 */
public class DBMViewLogTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBMaterializedViewLog>> {

    @Override
    public DBObjectTemplate<DBMaterializedViewLog> buildForDoris() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBObjectTemplate<DBMaterializedViewLog> buildForMySQL() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBObjectTemplate<DBMaterializedViewLog> buildForOBMySQL() {
        return new OBMySQLMViewLogTemplate();
    }

    @Override
    public DBObjectTemplate<DBMaterializedViewLog> buildForOBOracle() {
        return new OBOracleMViewLogTemplate();
    }

    @Override
    public DBObjectTemplate<DBMaterializedViewLog> buildForOracle() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBObjectTemplate<DBMaterializedViewLog> buildForOdpSharding() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBObjectTemplate<DBMaterializedViewLog> buildForPostgres() {
        throw new UnsupportedOperationException("not support yet");
    }

}
