/*
 * Copyright (c) 2025 OceanBase.
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
import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLViewTemplate;
import com.oceanbase.tools.dbbrowser.template.mysql.MysqlMViewTemplate;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 23:15
 * @since: 4.3.4
 */
public class DBMViewTemplateFactory extends AbstractDBBrowserFactory<DBObjectTemplate<DBMView>> {

    @Override
    public DBObjectTemplate<DBMView> buildForDoris() {
        return new MysqlMViewTemplate();
    }

    @Override
    public DBObjectTemplate<DBMView> buildForMySQL() {
        return null;
    }

    @Override
    public DBObjectTemplate<DBMView> buildForOBMySQL() {
        return new MysqlMViewTemplate();
    }

    @Override
    public DBObjectTemplate<DBMView> buildForOBOracle() {
        return null;
    }

    @Override
    public DBObjectTemplate<DBMView> buildForOracle() {
        return null;
    }

    @Override
    public DBObjectTemplate<DBMView> buildForOdpSharding() {
        return null;
    }

    @Override
    public DBObjectTemplate<DBMView> buildForPostgres() {
        return null;
    }
}