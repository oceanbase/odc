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

import com.oceanbase.tools.dbbrowser.template.BasePLTemplate;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLFunctionTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleFunctionTemplate;

/**
 * @author jingtian
 * @date 2024/5/22
 */
public class DBFunctionTemplateGenerator extends DBObjectTemplateGenerator<BasePLTemplate> {
    @Override
    public MySQLFunctionTemplate createForMySQL(String dbVersion) {
        return new MySQLFunctionTemplate();
    }

    @Override
    public OracleFunctionTemplate createForOracle(String dbVersion) {
        return new OracleFunctionTemplate();
    }
}
