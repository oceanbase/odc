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
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.template.mysql.MysqlMViewTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleMViewTemplate;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 23:15
 * @since: 4.3.4
 */
public class DBMViewTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBMaterializedView>> {

    @Override
    public DBObjectTemplate<DBMaterializedView> buildForDoris() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBObjectTemplate<DBMaterializedView> buildForMySQL() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBObjectTemplate<DBMaterializedView> buildForOBMySQL() {
        return new MysqlMViewTemplate();
    }

    @Override
    public DBObjectTemplate<DBMaterializedView> buildForOBOracle() {
        return new OracleMViewTemplate();
    }

    @Override
    public DBObjectTemplate<DBMaterializedView> buildForOracle() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBObjectTemplate<DBMaterializedView> buildForOdpSharding() {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBObjectTemplate<DBMaterializedView> buildForPostgres() {
        throw new UnsupportedOperationException("not support yet");
    }
}
