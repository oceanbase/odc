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
package com.oceanbase.tools.dbbrowser.parser.result;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModePLParserListener;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModeParserListener;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wenniu.ly
 * @date 2021/8/25
 */

@Getter
@Setter
public class ParseOraclePLResult extends ParsePLResult {

    // for parsing package
    private List<DBProcedure> procedureList;
    private List<DBFunction> functionList;

    public ParseOraclePLResult(OracleModePLParserListener listener) {
        super(listener);
        this.varibaleList = listener.getVaribaleList();
        this.typeList = listener.getTypeList();
        this.returnType = listener.getReturnType();
        this.plName = listener.getPlName();
        this.plType = listener.getPlType();
        this.isOrAs = listener.getIsOrAs();
        this.procedureList = listener.getProcedureList();
        this.functionList = listener.getFunctionList();
        this.cursorList = listener.getCursorList();
        this.empty = listener.isEmpty();
    }

    public ParseOraclePLResult(OracleModeParserListener listener) {
        super(listener);
        this.varibaleList = listener.getVaribaleList();
        this.typeList = listener.getTypeList();
        this.cursorList = listener.getCursorList();
        this.procedureList = listener.getProcedureList();
        this.functionList = listener.getFunctionList();
        this.cursorList = listener.getCursorList();
        this.plName = listener.getPlName();
        this.plType = listener.getPlType();
        this.isOrAs = listener.getIsOrAs();
    }

    public boolean containsFunction(String functionName) {
        if (StringUtils.isBlank(functionName)) {
            return false;
        }
        for (DBFunction function : functionList) {
            if (function.getFunName().equals(functionName)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsProcedure(String procedureName) {
        if (StringUtils.isBlank(procedureName)) {
            return false;
        }
        for (DBProcedure procedure : procedureList) {
            if (procedure.getProName().equals(procedureName)) {
                return true;
            }
        }
        return false;
    }
}
