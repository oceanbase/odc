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

import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleTriggerTemplate;

/**
 * @author jingtian
 * @date 2024/5/22
 */
public class DBTriggerTemplateGenerator extends DBObjectTemplateGenerator<DBObjectTemplate> {
    @Override
    public DBObjectTemplate createForMySQL(String dbVersion) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public OracleTriggerTemplate createForOracle(String dbVersion) {
        return new OracleTriggerTemplate();
    }
}
