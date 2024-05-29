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
package com.oceanbase.tools.dbbrowser.template.generator;

import com.oceanbase.tools.dbbrowser.template.BaseViewTemplate;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLViewTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleViewTemplate;

/**
 * @author jingtian
 * @date 2024/5/22
 */
public class DBViewTemplateGenerator extends DBObjectTemplateGenerator<BaseViewTemplate> {
    @Override
    public MySQLViewTemplate createForMySQL(String dbVersion) {
        return new MySQLViewTemplate();
    }

    @Override
    public OracleViewTemplate createForOracle(String dbVersion) {
        return new OracleViewTemplate();
    }
}
