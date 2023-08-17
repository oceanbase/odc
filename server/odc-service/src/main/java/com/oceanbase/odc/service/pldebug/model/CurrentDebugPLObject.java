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
package com.oceanbase.odc.service.pldebug.model;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModePLParserListener;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModeParserListener;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CurrentDebugPLObject extends ParseOraclePLResult {
    private String basicPlObjectName;
    private DBObjectType basicPlObjectType;

    public CurrentDebugPLObject(OracleModePLParserListener listener) {
        super(listener);
    }

    public CurrentDebugPLObject(OracleModeParserListener listener) {
        super(listener);
    }
}
