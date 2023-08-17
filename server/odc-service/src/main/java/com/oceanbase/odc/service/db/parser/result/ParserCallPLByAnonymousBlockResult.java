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
package com.oceanbase.odc.service.db.parser.result;

import java.util.Map;

import com.oceanbase.odc.service.db.model.AnonymousBlockFunctionCall;
import com.oceanbase.odc.service.db.model.AnonymousBlockProcedureCall;
import com.oceanbase.odc.service.db.model.PLParameter;
import com.oceanbase.odc.service.db.model.PLVariable;
import com.oceanbase.odc.service.db.parser.listener.OBOracleCallPLByAnonymousBlockListener;

import lombok.Getter;

@Getter
public class ParserCallPLByAnonymousBlockResult {

    protected Map<String, AnonymousBlockFunctionCall> functionCallMap;
    protected Map<String, AnonymousBlockProcedureCall> procedureCallMap;
    protected Map<String, PLVariable> variablesMap;
    protected Map<String, PLParameter> outParameter;

    public ParserCallPLByAnonymousBlockResult(OBOracleCallPLByAnonymousBlockListener listener) {
        this.procedureCallMap = listener.getProcedureCallMap();
        this.functionCallMap = listener.getFunctionCallMap();
        this.variablesMap = listener.getVariablesMap();
        this.outParameter = listener.getOutParameter();
    }
}
