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
package com.oceanbase.tools.dbbrowser.parser.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBPLType;
import com.oceanbase.tools.dbbrowser.model.DBPLVariable;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.parser.constant.PLObjectType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.util.StringUtils;
import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Alter_functionContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Alter_indexContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Alter_packageContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Alter_procedureContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Alter_userContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Anonymous_blockContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Commit_statementContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Create_function_bodyContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Create_indexContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Create_packageContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Create_package_bodyContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Create_procedure_bodyContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Create_triggerContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Create_typeContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Create_userContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Cursor_declarationContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Data_manipulation_language_statementsContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Drop_functionContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Drop_indexContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Drop_packageContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Drop_procedureContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Drop_triggerContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Drop_typeContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Function_bodyContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Function_nameContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Function_specContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.ParameterContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Procedure_bodyContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Procedure_nameContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Procedure_specContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Type_declarationContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser.Variable_declarationContext;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParserBaseListener;

import lombok.Getter;

/**
 * @author wenniu.ly
 * @date 2021/12/21
 */

@Getter
public class OracleModeParserListener extends PlSqlParserBaseListener implements BasicParserListener {
    private SqlType sqlType;
    private DBObjectType dbObjectType;
    private List<String> dbObjectNameList = new ArrayList<>();

    private String plName; // pl对象名
    private String plType;// pl类型，function、procedure、package
    private List<DBPLVariable> varibaleList = new ArrayList<>();
    private List<DBPLType> typeList = new ArrayList<>();
    private List<DBPLType> cursorList = new ArrayList<>();
    private List<DBProcedure> procedureList = new ArrayList<>();
    private List<DBFunction> functionList = new ArrayList<>();
    private String isOrAs;

    @Override
    public void enterCreate_package(Create_packageContext ctx) {
        TerminalNode is = ctx.IS();
        this.isOrAs = is == null ? "AS" : is.getText();
        doRecord(SqlType.CREATE, DBObjectType.PACKAGE, ctx.package_name(0).getText());
    }

    @Override
    public void enterCreate_package_body(Create_package_bodyContext ctx) {
        doRecord(SqlType.CREATE, DBObjectType.PACKAGE_BODY, ctx.package_name(0).getText());
    }

    @Override
    public void enterAlter_package(Alter_packageContext ctx) {
        DBObjectType dbObjectType = null;
        if (Objects.nonNull(ctx.BODY())) {
            this.dbObjectType = DBObjectType.PACKAGE_BODY;
        } else {
            this.dbObjectType = DBObjectType.PACKAGE;
        }
        doRecord(SqlType.ALTER, dbObjectType, ctx.package_name().getText());
    }

    @Override
    public void enterDrop_package(Drop_packageContext ctx) {
        DBObjectType dbObjectType = null;
        if (Objects.nonNull(ctx.BODY())) {
            this.dbObjectType = DBObjectType.PACKAGE_BODY;
        } else {
            this.dbObjectType = DBObjectType.PACKAGE;
        }
        doRecord(SqlType.DROP, dbObjectType, ctx.package_name().getText());
    }

    @Override
    public void enterDrop_procedure(Drop_procedureContext ctx) {
        doRecord(SqlType.DROP, DBObjectType.PROCEDURE, ctx.procedure_name().getText());
    }

    @Override
    public void enterAlter_procedure(Alter_procedureContext ctx) {
        doRecord(SqlType.ALTER, DBObjectType.PROCEDURE, ctx.procedure_name().getText());
    }

    @Override
    public void enterCreate_procedure_body(Create_procedure_bodyContext ctx) {
        List<DBPLParam> plParamList = handleParameters(ctx.parameter());

        String proName = ctx.procedure_name().getText();
        String proNameWithoutQuotation = this.handleObjectName(proName);
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(handleUpperCase(proName, proNameWithoutQuotation));
        dbProcedure.setParams(plParamList);
        dbProcedure.setStartline(ctx.getStart().getLine());
        dbProcedure.setStopLine(ctx.getStop().getLine());
        dbProcedure.setDdl(getDdl(ctx));
        this.procedureList.add(dbProcedure);
        doRecord(SqlType.CREATE, DBObjectType.PROCEDURE, proName);
    }

    @Override
    public void enterProcedure_name(Procedure_nameContext ctx) {
        this.plName = this.handleObjectName(ctx.getText());
    }

    @Override
    public void enterProcedure_spec(Procedure_specContext ctx) {
        List<DBPLParam> plParamList = handleParameters(ctx.parameter());

        String proName = ctx.identifier().getText();
        String proNameWithoutQuotation = this.handleObjectName(proName);
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(handleUpperCase(proName, proNameWithoutQuotation));
        dbProcedure.setParams(plParamList);
        dbProcedure.setStartline(ctx.getStart().getLine());
        dbProcedure.setStopLine(ctx.getStop().getLine());
        dbProcedure.setDdl(getDdl(ctx));
        this.procedureList.add(dbProcedure);
        doRecord(null, DBObjectType.PROCEDURE, proName);
    }

    @Override
    public void enterProcedure_body(Procedure_bodyContext ctx) {
        List<DBPLParam> plParamList = handleParameters(ctx.parameter());

        String proName = ctx.identifier().getText();
        String proNameWithoutQuotation = this.handleObjectName(proName);
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(handleUpperCase(proName, proNameWithoutQuotation));
        dbProcedure.setParams(plParamList);
        dbProcedure.setStartline(ctx.getStart().getLine());
        dbProcedure.setStopLine(ctx.getStop().getLine());
        dbProcedure.setDdl(getDdl(ctx));
        this.procedureList.add(dbProcedure);
        doRecord(null, DBObjectType.PROCEDURE, proName);
    }

    @Override
    public void enterDrop_function(Drop_functionContext ctx) {
        doRecord(SqlType.DROP, DBObjectType.FUNCTION, ctx.function_name().getText());
    }

    @Override
    public void enterAlter_function(Alter_functionContext ctx) {
        doRecord(SqlType.ALTER, DBObjectType.FUNCTION, ctx.function_name().getText());
    }

    @Override
    public void enterCreate_function_body(Create_function_bodyContext ctx) {
        List<DBPLParam> plParamList = handleParameters(ctx.parameter());

        String funName = ctx.function_name().getText();
        String funNameWithoutQuotation = this.handleObjectName(funName);
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(handleUpperCase(funName, funNameWithoutQuotation));
        dbFunction.setParams(plParamList);
        dbFunction.setReturnType(ctx.type_spec().getText());
        dbFunction.setStartline(ctx.getStart().getLine());
        dbFunction.setStopLine(ctx.getStop().getLine());
        dbFunction.setDdl(getDdl(ctx));
        this.functionList.add(dbFunction);
        doRecord(SqlType.CREATE, DBObjectType.FUNCTION, funName);
    }

    @Override
    public void enterFunction_name(Function_nameContext ctx) {
        this.plName = this.handleObjectName(ctx.getText());
    }

    @Override
    public void enterFunction_spec(Function_specContext ctx) {
        List<DBPLParam> plParamList = handleParameters(ctx.parameter());

        String funName = ctx.identifier().getText();
        String funNameWithoutQuotation = this.handleObjectName(funName);
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(handleUpperCase(funName, funNameWithoutQuotation));
        dbFunction.setParams(plParamList);
        dbFunction.setReturnType(ctx.type_spec().getText());
        dbFunction.setStartline(ctx.getStart().getLine());
        dbFunction.setStopLine(ctx.getStop().getLine());
        dbFunction.setDdl(getDdl(ctx));
        this.functionList.add(dbFunction);
        doRecord(null, DBObjectType.FUNCTION, funName);
    }

    @Override
    public void enterFunction_body(Function_bodyContext ctx) {
        List<DBPLParam> plParamList = handleParameters(ctx.parameter());

        String funName = ctx.identifier().getText();
        String funNameWithoutQuotation = this.handleObjectName(funName);
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(handleUpperCase(funName, funNameWithoutQuotation));
        dbFunction.setParams(plParamList);
        dbFunction.setReturnType(ctx.type_spec().getText());
        dbFunction.setStartline(ctx.getStart().getLine());
        dbFunction.setStopLine(ctx.getStop().getLine());
        dbFunction.setDdl(getDdl(ctx));
        this.functionList.add(dbFunction);
        doRecord(null, DBObjectType.FUNCTION, funName);
    }

    @Override
    public void enterVariable_declaration(Variable_declarationContext ctx) {
        DBPLVariable variable = generatePLVariable(ctx);
        this.varibaleList.add(variable);
    }

    @Override
    public void enterType_declaration(Type_declarationContext ctx) {
        String typeVar = ctx.identifier().getText();
        String typeName = null;
        if (Objects.nonNull(ctx.table_type_def())) {
            typeName = PLObjectType.TABLE_TYPE.name();
        } else if (Objects.nonNull(ctx.varray_type_def())) {
            typeName = PLObjectType.VARRAY_TYPE.name();
        } else if (Objects.nonNull(ctx.ref_cursor_type_def())) {
            typeName = PLObjectType.CURSOR_TYPE.name();
        } else if (Objects.nonNull(ctx.record_type_def())) {
            typeName = PLObjectType.RECORD_TYPE.name();
        }
        DBPLType plType = new DBPLType();
        plType.setTypeVariable(handleObjectName(typeVar));
        plType.setTypeName(typeName);
        this.typeList.add(plType);
    }

    @Override
    public void enterCursor_declaration(Cursor_declarationContext ctx) {
        String cursorVar = ctx.identifier().getText();
        String cursorName = PLObjectType.CURSOR.name();
        DBPLType plType = new DBPLType();
        plType.setTypeVariable(handleObjectName(cursorVar));
        plType.setTypeName(cursorName);
        plType.setDdl(getDdl(ctx));
        this.cursorList.add(plType);
    }

    @Override
    public void enterAnonymous_block(Anonymous_blockContext ctx) {
        if (Objects.isNull(this.plType)) {
            this.plType = DBObjectType.ANONYMOUS_BLOCK.getName();
        }
        doRecord(SqlType.CREATE, DBObjectType.ANONYMOUS_BLOCK, null);
    }

    private List<DBPLParam> handleParameters(List<ParameterContext> parameterContextList) {
        List<DBPLParam> plParamList = new ArrayList<>();
        for (int i = 0; i < parameterContextList.size(); i++) {
            ParameterContext currentContext = parameterContextList.get(i);
            DBPLParam plParam = new DBPLParam();
            plParam.setParamName(StringUtils.unquoteOracleIdentifier(currentContext.parameter_name().getText()));
            plParam.setSeqNum(i + 1);
            plParam.setDataType(currentContext.type_spec().getText());
            boolean hasInParam = CollectionUtils.isNotEmpty(currentContext.IN());
            boolean hasOutParam = CollectionUtils.isNotEmpty(currentContext.OUT());
            boolean hasInOutParam = CollectionUtils.isNotEmpty(currentContext.INOUT());
            if ((hasInParam && hasOutParam) || hasInOutParam) {
                plParam.setParamMode(DBPLParamMode.INOUT);
            } else if (hasOutParam) {
                plParam.setParamMode(DBPLParamMode.OUT);
            } else {
                // if param does not have any in / out / in out keywords, param mode falls to default IN mode
                plParam.setParamMode(DBPLParamMode.IN);
            }
            if (Objects.nonNull(currentContext.default_value_part())) {
                plParam.setDefaultValue(currentContext.default_value_part().expression().getText());
            }
            plParamList.add(plParam);
        }
        return plParamList;
    }

    private DBPLVariable generatePLVariable(Variable_declarationContext ctx) {
        String var = ctx.identifier().getText();
        String type = ctx.type_spec().getText();
        DBPLVariable variable = new DBPLVariable();
        variable.setVarName(handleObjectName(var));
        variable.setVarType(type);
        return variable;
    }

    private String handleObjectName(String name) {
        return StringUtils.unquoteOracleIdentifier(name);
    }

    private String handleUpperCase(String originalName, String name) {
        if (originalName.equals(name)) {
            // original name with out quotation
            return name.toUpperCase();
        }
        return name;
    }

    @Override
    public void enterDrop_trigger(Drop_triggerContext ctx) {
        doRecord(SqlType.DROP, DBObjectType.TRIGGER, ctx.trigger_name().getText());
    }

    @Override
    public void enterCreate_trigger(Create_triggerContext ctx) {
        doRecord(SqlType.CREATE, DBObjectType.TRIGGER, ctx.trigger_name().getText());
    }

    @Override
    public void enterCreate_type(Create_typeContext ctx) {
        String rawTypeName = null;
        if (Objects.nonNull(ctx.type_body())) {
            rawTypeName = ctx.type_body().type_name().getText();
        } else if (Objects.nonNull(ctx.type_definition())) {
            rawTypeName = ctx.type_definition().type_name().getText();
        }
        doRecord(SqlType.CREATE, DBObjectType.TYPE, rawTypeName);
    }

    @Override
    public void enterDrop_type(Drop_typeContext ctx) {
        doRecord(SqlType.DROP, DBObjectType.TYPE, ctx.type_name().getText());
    }

    @Override
    public void enterCreate_index(Create_indexContext ctx) {
        doRecord(SqlType.CREATE, DBObjectType.INDEX, ctx.index_name().getText());
    }

    @Override
    public void enterDrop_index(Drop_indexContext ctx) {
        doRecord(SqlType.DROP, DBObjectType.INDEX, ctx.index_name().getText());
    }

    @Override
    public void enterAlter_index(Alter_indexContext ctx) {
        doRecord(SqlType.ALTER, DBObjectType.INDEX, ctx.index_name().getText());
    }

    @Override
    public void enterCreate_user(Create_userContext ctx) {
        doRecord(SqlType.CREATE, DBObjectType.USER, ctx.user_object_name().getText());
    }

    @Override
    public void enterAlter_user(Alter_userContext ctx) {
        doRecord(SqlType.ALTER, DBObjectType.USER, ctx.user_object_name().get(0).getText());
    }

    @Override
    public void enterData_manipulation_language_statements(Data_manipulation_language_statementsContext ctx) {
        if (Objects.nonNull(ctx.select_statement()) && !ctx.select_statement().isEmpty()) {
            doRecord(SqlType.SELECT, null, null);
        }
    }

    @Override
    public void enterCommit_statement(Commit_statementContext ctx) {
        doRecord(SqlType.COMMIT, DBObjectType.OTHERS, null);
    }

    @Override
    public void enterRollback_statement(PlSqlParser.Rollback_statementContext ctx) {
        doRecord(SqlType.ROLLBACK, DBObjectType.OTHERS, null);
    }

    @Override
    public void enterComment_on_column(PlSqlParser.Comment_on_columnContext ctx) {
        doRecord(SqlType.COMMENT_ON, DBObjectType.COLUMN, null);
    }

    @Override
    public void enterComment_on_table(PlSqlParser.Comment_on_tableContext ctx) {
        doRecord(SqlType.COMMENT_ON, DBObjectType.TABLE, null);
    }

    @Override
    public void enterComment_on_materialized(PlSqlParser.Comment_on_materializedContext ctx) {
        doRecord(SqlType.COMMENT_ON, DBObjectType.OTHERS, null);
    }

    @Override
    public void enterProcedure_call(PlSqlParser.Procedure_callContext ctx) {
        if (ctx.CALL() != null) {
            doRecord(SqlType.CALL, DBObjectType.PROCEDURE, null);
        }
    }

    private void doRecord(SqlType sqlType, DBObjectType dbObjectType, String rawPlName) {
        if (Objects.nonNull(sqlType) && Objects.isNull(this.sqlType)) {
            this.sqlType = sqlType;
        }
        if (Objects.nonNull(dbObjectType) && Objects.isNull(this.dbObjectType)) {
            this.dbObjectType = dbObjectType;
            this.plType = dbObjectType.getName();
        }
        if (Objects.nonNull(rawPlName) && Objects.isNull(this.plName)) {
            this.plName = this.handleObjectName(rawPlName);
        }
    }

    @Override
    public void enterAlter_session(PlSqlParser.Alter_sessionContext ctx) {
        doRecord(SqlType.ALTER_SESSION, DBObjectType.OTHERS, null);
    }

    @Override
    public void enterScope_or_scope_alias(PlSqlParser.Scope_or_scope_aliasContext ctx) {
        if (ctx.SESSION() != null) {
            doRecord(SqlType.SET_SESSION, DBObjectType.OTHERS, null);
        }
    }

    private String getDdl(ParserRuleContext ctx) {
        Token start = ctx.getStart();
        Token stop = ctx.getStop();
        // remove SEMICOLON
        int end = stop.getType() == PlSqlLexer.SEMICOLON ? (stop.getStopIndex() - 1) : stop.getStopIndex();
        CharStream stream = start.getTokenSource().getInputStream();
        return stream.getText(Interval.of(start.getStartIndex(), end));
    }

}
