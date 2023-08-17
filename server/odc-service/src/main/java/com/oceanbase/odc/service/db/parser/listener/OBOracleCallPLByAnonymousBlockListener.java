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
package com.oceanbase.odc.service.db.parser.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.service.db.model.AnonymousBlockFunctionCall;
import com.oceanbase.odc.service.db.model.AnonymousBlockProcedureCall;
import com.oceanbase.odc.service.db.model.PLParameter;
import com.oceanbase.odc.service.db.model.PLVariable;
import com.oceanbase.tools.dbbrowser.util.StringUtils;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.ArgumentContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Assignment_statementContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Function_argumentContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Function_callContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.General_element_partContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Variable_declarationContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParserBaseListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OBOracleCallPLByAnonymousBlockListener extends PlSqlParserBaseListener {
    private final Context context = new Context();
    private boolean isInAssignment = false;
    private Map<String, AnonymousBlockFunctionCall> functionCallMap = new HashMap<>();
    private Map<String, AnonymousBlockProcedureCall> procedureCallMap = new HashMap<>();
    private Map<String, PLVariable> variablesMap = new HashMap<>();
    private Map<String, PLParameter> outParameter = new HashMap<>();

    @Override
    public void enterFunction_call(Function_callContext ctx) {
        context.setProcedureName(
                handleObjectName(ctx.routine_name().getChild(ctx.routine_name().getChildCount() - 1).getText()));
        context.setCallLine(ctx.routine_name().identifier().start.getLine());
    }

    @Override
    public void exitFunction_call(Function_callContext ctx) {
        context.setProcedureName(null);
        context.setCallLine(-1);
    }

    @Override
    public void enterFunction_argument(Function_argumentContext ctx) {
        if (context.getProcedureName() != null) {
            AnonymousBlockProcedureCall procedureCall = new AnonymousBlockProcedureCall();
            Map<String, PLParameter> params = processParameters(ctx);
            String plName = context.getProcedureName();
            procedureCall.setParams(params);
            procedureCall.setCallLine(context.getCallLine());
            procedureCall.setProcedureName(plName);
            this.procedureCallMap.put(plName, procedureCall);
        } else if (context.getFunctionName() != null) {
            AnonymousBlockFunctionCall functionCall = new AnonymousBlockFunctionCall();
            Map<String, PLParameter> params = processParameters(ctx);
            PLParameter returnValue = new PLParameter();
            returnValue.setParamName(context.getReturnValue());
            returnValue.setBeginIndex(context.getReturnValueBegin());
            returnValue.setEndIndex(context.getReturnValueEnd());
            String plName = context.getFunctionName();
            functionCall.setParams(params);
            functionCall.setCallLine(context.getCallLine());
            functionCall.setFunctionName(plName);
            functionCall.setReturnParam(returnValue);
            this.functionCallMap.put(plName, functionCall);
        }
    }

    @Override
    public void enterAssignment_statement(Assignment_statementContext ctx) {
        this.isInAssignment = true;
        if (ctx.general_element() != null) {
            context.setCallLine(ctx.general_element().getStart().getLine());
            context.setReturnValue(ctx.general_element().getText());
            context.setReturnValueBegin(ctx.general_element().getStart().getStartIndex());
            context.setReturnValueEnd(ctx.general_element().getStop().getStopIndex());
            if (Objects.equals(ctx.general_element().getText(), ctx.expression().getText())) {
                PLParameter plParameter = new PLParameter();
                plParameter.setBeginIndex(ctx.general_element().getStart().getStartIndex());
                plParameter.setEndIndex(ctx.general_element().getStop().getStopIndex());
                plParameter.setParamName(ctx.general_element().getText());
                this.outParameter.put(ctx.general_element().getText(), plParameter);
            }
        }
    }

    @Override
    public void exitAssignment_statement(Assignment_statementContext ctx) {
        this.isInAssignment = false;
        context.setCallLine(-1);
        context.setReturnValue(null);
        context.setReturnValueBegin(-1);
        context.setReturnValueEnd(-1);
    }

    @Override
    public void enterGeneral_element_part(General_element_partContext ctx) {
        if (this.isInAssignment) {
            context.setFunctionName(handleObjectName(ctx.id_expression(ctx.id_expression().size() - 1).getText()));
        }
    }

    @Override
    public void exitGeneral_element_part(General_element_partContext ctx) {
        if (this.isInAssignment) {
            context.setFunctionName(null);
        }
    }

    @Override
    public void enterVariable_declaration(Variable_declarationContext ctx) {
        PLVariable variable = generatePLVariable(ctx);
        this.variablesMap.put(variable.getName(), variable);
    }

    private Map<String, PLParameter> processParameters(Function_argumentContext ctx) {
        Map<String, PLParameter> params = new HashMap<>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof PlSqlParser.ArgumentContext) {
                PlSqlParser.ArgumentContext paramCtx = ((ArgumentContext) ctx.getChild(i));
                PLParameter param = new PLParameter();
                if (paramCtx.getChildCount() != 4) {
                    throw new BadArgumentException(ErrorCodes.ExecPLByAnonymousBlockErrorFormatParameters,
                            new Object[] {paramCtx.getStart().getLine()}, null);
                }
                if (!paramCtx.identifier().getText().equals(paramCtx.expression().getText())) {
                    throw new BadArgumentException(ErrorCodes.ExecPLByAnonymousBlockDifferentParameters,
                            new Object[] {paramCtx.getStart().getLine(), paramCtx.identifier().getText(),
                                    paramCtx.expression().getText()},
                            null);
                }
                if (this.variablesMap.get(paramCtx.identifier().getText()) == null) {
                    throw new BadArgumentException(ErrorCodes.ExecPLByAnonymousBlockUndefinedParameters,
                            new Object[] {paramCtx.getStart().getLine(), paramCtx.identifier().getText()}, null);
                }
                param.setParamName(paramCtx.identifier().getText());
                param.setBeginIndex(paramCtx.getStart().getStartIndex());
                param.setEndIndex(paramCtx.getStop().getStopIndex());
                params.put(paramCtx.identifier().getText(), param);
            }
        }
        return params;
    }

    private PLVariable generatePLVariable(Variable_declarationContext ctx) {
        PLVariable variable = new PLVariable();
        String name = ctx.identifier().getText();
        String type = ctx.type_spec().getText();
        if (ctx.default_value_part() != null) {
            String value = ctx.default_value_part().expression().getText();
            variable.setValue(value);
        }
        variable.setName(name);
        variable.setType(type);
        return variable;
    }

    private String handleObjectName(String name) {
        if (StringUtils.endsWith(name, "\"") && StringUtils.startsWith(name, "\"")) {
            return StringUtils.unquoteOracleIdentifier(name);
        }
        return name.toUpperCase();
    }

    @Setter
    @Getter
    static class Context {
        private String procedureName = null;
        private String functionName = null;
        private String returnValue = null;
        private int returnValueBegin = -1;
        private int returnValueEnd = -1;
        private int callLine = -1;
    }
}
