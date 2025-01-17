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
import org.antlr.v4.runtime.tree.ParseTree;

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
import com.oceanbase.tools.sqlparser.oboracle.PLParser;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Alter_function_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Alter_package_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Alter_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Anonymous_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Attr_specContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_function_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_package_body_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_package_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_trigger_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_type_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Cursor_declContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Drop_function_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Drop_package_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Drop_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Drop_trigger_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Drop_type_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Func_declContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.IdentifierContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Is_or_asContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Object_type_defContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Package_blockContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Pl_schema_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Plsql_function_sourceContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Plsql_procedure_sourceContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Proc_declContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Record_type_defContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Ref_cursor_type_defContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Var_declContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParserBaseListener;

import lombok.Getter;

@Getter
public class OracleModePLParserListener extends PLParserBaseListener implements BasicParserListener {
    private List<DBPLVariable> varibaleList = new ArrayList<>();
    private List<DBPLType> typeList = new ArrayList<>();
    private String returnType;
    // for parsing package
    private List<DBProcedure> procedureList = new ArrayList<>();
    private List<DBFunction> functionList = new ArrayList<>();
    private List<DBPLType> cursorList = new ArrayList<>();
    private String plName; // pl对象名
    private String plType;// pl类型，function、procedure、package
    private String isOrAs;

    private SqlType sqlType;
    private DBObjectType dbObjectType;
    private List<String> dbObjectNameList = new ArrayList<>();

    public boolean isEmpty() {
        return varibaleList.isEmpty() && typeList.isEmpty()
                && procedureList.isEmpty() && functionList.isEmpty() && cursorList.isEmpty()
                && StringUtils.isEmpty(returnType) && StringUtils.isEmpty(plName)
                && StringUtils.isEmpty(plType);
    }

    @Override
    public void enterAnonymous_stmt(Anonymous_stmtContext ctx) {
        this.plType = DBObjectType.ANONYMOUS_BLOCK.getName();
        this.sqlType = SqlType.OTHERS;
    }

    @Override
    public void enterPl_schema_name(Pl_schema_nameContext ctx) {
        if (this.plName == null) {
            String plName = ctx.getText();
            this.plName = handleObjectName(plName);
            List<ParseTree> children = ctx.getParent().children;
            // 语法上看，类型不可能是第一个token，这里从第二个token遍历
            for (int i = 1; i < children.size(); i++) {
                if (children.get(i).getText().equalsIgnoreCase(plName)) {
                    this.plType = children.get(i - 1).getText();
                    if ("body".equalsIgnoreCase(this.plType)) {
                        this.plType = DBObjectType.PACKAGE_BODY.getName();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void enterPackage_block(Package_blockContext ctx) {
        Is_or_asContext isOrAs = ctx.is_or_as();
        if (isOrAs != null) {
            this.isOrAs = isOrAs.getText();
        }
    }

    @Override
    public void enterPlsql_procedure_source(Plsql_procedure_sourceContext ctx) {
        for (int j = 0; j < ctx.getChildCount(); j++) {
            if (ctx.getChild(j) instanceof PLParser.Sp_param_listContext) {
                List<DBPLParam> plParamList = new ArrayList<>();
                PLParser.Sp_param_listContext paramlist = (PLParser.Sp_param_listContext) ctx.getChild(j);
                if (paramlist != null) {
                    // procedure param expression count
                    int count = paramlist.getChildCount();
                    // procedure param count
                    int paramCount = 0;
                    for (int i = 0; i < count; i++) {
                        if (paramlist.getChild(i) instanceof PLParser.Sp_paramContext) {
                            PLParser.Sp_paramContext paramContext = (PLParser.Sp_paramContext) paramlist.getChild(i);
                            DBPLParam plParam = new DBPLParam();
                            String paramName = paramContext.param_name().getText();
                            plParam.setParamName(StringUtils.unquoteOracleIdentifier(paramName));
                            plParam.setSeqNum(++paramCount);
                            plParam.setDataType(paramContext.pl_outer_data_type().getText());
                            PLParser.Default_optContext defaultExpr = paramContext.default_opt();
                            if (defaultExpr != null) {
                                // 第一个节点是静态标识"default"
                                PLParser.Default_exprContext boolExprContext =
                                        (PLParser.Default_exprContext) defaultExpr.getChild(1);
                                // TODO 这里默认值解析出来有<EOF>结束符，需要优化语法文件
                                String defValue = boolExprContext.getText().replace("<EOF>", "");
                                plParam.setDefaultValue(defValue);
                            }
                            int cnt = paramContext.getChildCount();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int k = 1; k < cnt; k++) {
                                if (!(paramContext.getChild(k) instanceof PLParser.Pl_outer_data_typeContext)) {
                                    stringBuilder.append(paramContext.getChild(k).getText()).append(" ");
                                } else {
                                    break;
                                }
                            }
                            String modeStr = stringBuilder.toString().trim();
                            DBPLParamMode mode;
                            if (StringUtils.isBlank(modeStr)) {
                                mode = DBPLParamMode.IN; // 默认值，sql中可不显式展示
                            } else {
                                mode = DBPLParamMode.getEnum(modeStr);
                            }
                            plParam.setParamMode(mode);
                            plParamList.add(plParam);
                        }
                    }
                }

                String proName = ctx.pl_schema_name().getText();
                String proNameWithoutQuotation = this.handleObjectName(proName);
                DBProcedure dbProcedure = new DBProcedure();
                dbProcedure.setProName(handleUpperCase(proName, proNameWithoutQuotation));
                dbProcedure.setParams(plParamList);
                this.procedureList.add(dbProcedure);
                break;
            }
        }
    }

    @Override
    public void enterPlsql_function_source(Plsql_function_sourceContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i).getText().equalsIgnoreCase("return")) {
                this.returnType = ctx.getChild(i + 1).getText();
            }
        }
        // 解析function参数
        for (int j = 0; j < ctx.getChildCount(); j++) {
            if (ctx.getChild(j) instanceof PLParser.Sp_param_listContext) {
                PLParser.Sp_param_listContext paramlist = (PLParser.Sp_param_listContext) ctx.getChild(j);

                List<DBPLParam> plParamList = new ArrayList<>();
                if (paramlist != null) {
                    // function param expression count
                    int count = paramlist.getChildCount();
                    // function param count
                    int paramCount = 0;
                    for (int i = 0; i < count; i++) {
                        if (paramlist.getChild(i) instanceof PLParser.Sp_paramContext) {
                            PLParser.Sp_paramContext paramContext = (PLParser.Sp_paramContext) paramlist.getChild(i);
                            DBPLParam plParam = new DBPLParam();
                            String paramName = paramContext.param_name().getText();
                            plParam.setParamName(StringUtils.unquoteOracleIdentifier(paramName));
                            plParam.setSeqNum(++paramCount);
                            plParam.setDataType(paramContext.pl_outer_data_type().getText());

                            PLParser.Default_optContext defaultExpr = paramContext.default_opt();
                            if (defaultExpr != null) {
                                // 第一个节点是静态标识"default"
                                PLParser.Default_exprContext boolExprContext =
                                        (PLParser.Default_exprContext) defaultExpr.getChild(1);
                                // TODO 这里默认值解析出来有<EOF>结束符，需要优化语法文件
                                String defValue = boolExprContext.getText().replace("<EOF>", "");
                                plParam.setDefaultValue(defValue);
                            }

                            int cnt = paramContext.getChildCount();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int k = 1; k < cnt; k++) {
                                if (!(paramContext.getChild(k) instanceof PLParser.Pl_outer_data_typeContext)) {
                                    stringBuilder.append(paramContext.getChild(k).getText()).append(" ");
                                } else {
                                    break;
                                }
                            }
                            String modeStr = stringBuilder.toString().trim();
                            DBPLParamMode mode;
                            if (StringUtils.isBlank(modeStr)) {
                                mode = DBPLParamMode.IN; // 默认值，sql中可不显式展示
                            } else {
                                mode = DBPLParamMode.getEnum(modeStr);
                            }
                            plParam.setParamMode(mode);
                            plParamList.add(plParam);
                        }
                    }
                }

                // not sure why pl_schema_name return multivalue, it should contains only one element according to
                // parser rrd with version of 3.0.0
                String funName = ctx.pl_schema_name(0).getText();
                String funNameWithoutQuotation = this.handleObjectName(funName);
                DBFunction dbFunction = new DBFunction();
                dbFunction.setFunName(handleUpperCase(funName, funNameWithoutQuotation));
                dbFunction.setParams(plParamList);
                dbFunction.setReturnType(ctx.pl_outer_data_type().getText());
                this.functionList.add(dbFunction);
                break;
            }
        }
    }

    @Override
    public void enterVar_decl(Var_declContext ctx) {
        DBPLVariable variable = generatePLVariable(ctx);
        this.varibaleList.add(variable);
    }

    private DBPLVariable generatePLVariable(Var_declContext ctx) {
        String var = ctx.var_common_name().getText();
        String type = ctx.pl_inner_data_type().getText();
        DBPLVariable variable = new DBPLVariable();
        variable.setVarName(handleObjectName(var));
        variable.setVarType(type);
        return variable;
    }

    @Override
    public void enterFunc_decl(Func_declContext ctx) {
        List<DBPLParam> plParamList = new ArrayList<>();
        PLParser.Sp_param_listContext paramlist = ctx.sp_param_list();
        if (paramlist != null) {
            // function param expression count
            int count = paramlist.getChildCount();
            // function param count
            int paramCount = 0;
            for (int i = 0; i < count; i++) {
                if (paramlist.getChild(i) instanceof PLParser.Sp_paramContext) {
                    PLParser.Sp_paramContext paramContext = (PLParser.Sp_paramContext) paramlist.getChild(i);
                    DBPLParam plParam = new DBPLParam();
                    String paramName = paramContext.param_name().getText();
                    plParam.setParamName(StringUtils.unquoteOracleIdentifier(paramName));
                    plParam.setSeqNum(++paramCount);
                    plParam.setDataType(paramContext.pl_outer_data_type().getText());
                    int cnt = paramContext.getChildCount();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int k = 1; k < cnt; k++) {
                        if (!(paramContext.getChild(k) instanceof PLParser.Pl_outer_data_typeContext)) {
                            stringBuilder.append(paramContext.getChild(k).getText()).append(" ");
                        } else {
                            break;
                        }
                    }
                    String modeStr = stringBuilder.toString().trim();
                    DBPLParamMode mode;
                    if (StringUtils.isBlank(modeStr)) {
                        mode = DBPLParamMode.IN; // 默认值，sql中可不显式展示
                    } else {
                        mode = DBPLParamMode.getEnum(modeStr);
                    }
                    plParam.setParamMode(mode);
                    plParamList.add(plParam);
                }
            }
        }
        String funName = ctx.func_name().getText();
        String funNameWithoutQuotation = this.handleObjectName(funName);
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(handleUpperCase(funName, funNameWithoutQuotation));
        dbFunction.setParams(plParamList);
        dbFunction.setReturnType(ctx.pl_outer_data_type().getText());
        dbFunction.setDdl(getDdl(ctx));
        this.functionList.add(dbFunction);
    }

    @Override
    public void enterProc_decl(Proc_declContext ctx) {
        List<DBPLParam> plParamList = new ArrayList<>();
        PLParser.Sp_param_listContext paramlist = ctx.sp_param_list();
        if (paramlist != null) {
            // procedure param expression count
            int count = paramlist.getChildCount();
            // procedure param count
            int paramCount = 0;
            for (int i = 0; i < count; i++) {
                if (paramlist.getChild(i) instanceof PLParser.Sp_paramContext) {
                    PLParser.Sp_paramContext paramContext = (PLParser.Sp_paramContext) paramlist.getChild(i);
                    DBPLParam plParam = new DBPLParam();
                    String paramName = paramContext.param_name().getText();
                    plParam.setParamName(StringUtils.unquoteOracleIdentifier(paramName));
                    plParam.setSeqNum(++paramCount);
                    plParam.setDataType(paramContext.pl_outer_data_type().getText());
                    int cnt = paramContext.getChildCount();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int k = 1; k < cnt; k++) {
                        if (!(paramContext.getChild(k) instanceof PLParser.Pl_outer_data_typeContext)) {
                            stringBuilder.append(paramContext.getChild(k).getText()).append(" ");
                        } else {
                            break;
                        }
                    }
                    String modeStr = stringBuilder.toString().trim();
                    DBPLParamMode mode;
                    if (StringUtils.isBlank(modeStr)) {
                        mode = DBPLParamMode.IN; // 默认值，sql中可不显式展示
                    } else {
                        mode = DBPLParamMode.getEnum(modeStr);
                    }
                    plParam.setParamMode(mode);
                    plParamList.add(plParam);
                }
            }
        }

        String proName = ctx.proc_name().getText();
        String proNameWithoutQuotation = this.handleObjectName(proName);
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(handleUpperCase(proName, proNameWithoutQuotation));
        dbProcedure.setParams(plParamList);
        dbProcedure.setDdl(getDdl(ctx));
        this.procedureList.add(dbProcedure);
    }

    @Override
    public void enterCursor_decl(Cursor_declContext ctx) {
        DBPLType cursor = new DBPLType();
        cursor.setTypeName(ctx.cursor_name().getText());
        cursor.setDdl(getDdl(ctx));
        cursorList.add(cursor);
    }

    @Override
    public void enterRef_cursor_type_def(Ref_cursor_type_defContext ctx) {
        String typeVar = ctx.type_name().identifier().getText();
        String typeName = PLObjectType.CURSOR_TYPE.name();
        DBPLType plType = new DBPLType();
        plType.setTypeVariable(handleObjectName(typeVar));
        plType.setTypeName(typeName);
        this.typeList.add(plType);
    }

    @Override
    public void enterRecord_type_def(Record_type_defContext ctx) {
        String typeVar = ctx.type_name().identifier().getText();
        String typeName = PLObjectType.RECORD_TYPE.name();
        DBPLType plType = new DBPLType();
        plType.setTypeVariable(handleObjectName(typeVar));
        plType.setTypeName(typeName);
        this.typeList.add(plType);
    }

    @Override
    public void enterObject_type_def(Object_type_defContext ctx) {
        // 增加plparser对type定义中变量的解析
        for (Attr_specContext attr_specContext : ctx.attr_and_element_spec().attr_list().attr_spec()) {
            String varName = attr_specContext.common_identifier().getText();
            String type = attr_specContext.pl_inner_data_type().getText();
            DBPLVariable variable = new DBPLVariable();
            variable.setVarName(handleObjectName(varName));
            variable.setVarType(type);
            this.varibaleList.add(variable);
        }
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
    public void enterDrop_procedure_stmt(Drop_procedure_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.PROCEDURE;
        for (IdentifierContext identifierContext : ctx.pl_schema_name().identifier()) {
            this.dbObjectNameList.add(handleObjectName(identifierContext.getText()));
        }
    }

    @Override
    public void enterCreate_procedure_stmt(Create_procedure_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.PROCEDURE;
        List<IdentifierContext> identifierContexts = ctx.plsql_procedure_source().pl_schema_name().identifier();
        for (IdentifierContext identifierContext : identifierContexts) {
            this.dbObjectNameList.add(handleUpperCaseObjectName(identifierContext.getText()));
        }
    }

    @Override
    public void enterCreate_package_stmt(Create_package_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.PACKAGE;
        List<IdentifierContext> identifierContexts = ctx.package_block().pl_schema_name().identifier();
        for (IdentifierContext identifierContext : identifierContexts) {
            this.dbObjectNameList.add(handleUpperCaseObjectName(identifierContext.getText()));
        }
    }

    @Override
    public void enterCreate_package_body_stmt(Create_package_body_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.PACKAGE_BODY;
        List<IdentifierContext> identifierContexts = ctx.package_body_block().pl_schema_name().identifier();
        for (IdentifierContext identifierContext : identifierContexts) {
            this.dbObjectNameList.add(handleUpperCaseObjectName(identifierContext.getText()));
        }
    }

    @Override
    public void enterCreate_function_stmt(Create_function_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.FUNCTION;
        for (Pl_schema_nameContext pl_schema_nameContext : ctx.plsql_function_source().pl_schema_name()) {
            for (IdentifierContext identifierContext : pl_schema_nameContext.identifier())
                this.dbObjectNameList.add(handleUpperCaseObjectName(identifierContext.getText()));
        }
    }

    @Override
    public void enterDrop_function_stmt(Drop_function_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.FUNCTION;
        this.dbObjectNameList.add(handleObjectName(ctx.pl_schema_name().getText()));
    }

    @Override
    public void enterDrop_trigger_stmt(Drop_trigger_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.TRIGGER;
        this.dbObjectNameList.add(handleObjectName(ctx.pl_schema_name().getText()));
    }

    @Override
    public void enterCreate_trigger_stmt(Create_trigger_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.TRIGGER;
        for (IdentifierContext identifierContext : ctx.plsql_trigger_source().pl_schema_name().identifier())
            this.dbObjectNameList.add(handleUpperCaseObjectName(identifierContext.getText()));
    }

    @Override
    public void enterCreate_type_stmt(Create_type_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.TYPE;
        this.dbObjectNameList.add(handleObjectName(ctx.plsql_type_spec_source().pl_schema_name().getText()));
    }

    @Override
    public void enterDrop_type_stmt(Drop_type_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.TYPE;
        this.dbObjectNameList.add(handleObjectName(ctx.pl_schema_name(0).getText()));
    }

    @Override
    public void enterDrop_package_stmt(Drop_package_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.PACKAGE;
        this.dbObjectNameList.add(handleObjectName(ctx.pl_schema_name().getText()));
    }

    private String handleUpperCaseObjectName(String objectName) {
        String proNameWithoutQuotation = this.handleObjectName(objectName);
        return handleUpperCase(objectName, proNameWithoutQuotation);
    }

    @Override
    public void enterAlter_procedure_stmt(Alter_procedure_stmtContext ctx) {
        this.sqlType = SqlType.ALTER;
        this.dbObjectType = DBObjectType.PROCEDURE;
        List<IdentifierContext> identifierContexts = ctx.pl_schema_name().identifier();
        for (IdentifierContext identifierContext : identifierContexts) {
            this.dbObjectNameList.add(handleUpperCaseObjectName(identifierContext.getText()));
        }
    }

    @Override
    public void enterAlter_function_stmt(Alter_function_stmtContext ctx) {
        this.sqlType = SqlType.ALTER;
        this.dbObjectType = DBObjectType.FUNCTION;
        List<IdentifierContext> identifierContexts = ctx.pl_schema_name().identifier();
        for (IdentifierContext identifierContext : identifierContexts) {
            this.dbObjectNameList.add(handleUpperCaseObjectName(identifierContext.getText()));
        }
    }

    @Override
    public void enterAlter_package_stmt(Alter_package_stmtContext ctx) {
        this.sqlType = SqlType.ALTER;
        if (Objects.nonNull(ctx.alter_package_clause().BODY())) {
            this.dbObjectType = DBObjectType.PACKAGE_BODY;
        } else {
            this.dbObjectType = DBObjectType.PACKAGE;
        }
        List<IdentifierContext> identifierContexts = ctx.pl_schema_name().identifier();
        for (IdentifierContext identifierContext : identifierContexts) {
            this.dbObjectNameList.add(handleUpperCaseObjectName(identifierContext.getText()));
        }
    }

    @Override
    public void enterCall_spec(PLParser.Call_specContext ctx) {
        this.sqlType = sqlType.CALL;
        this.dbObjectType = DBObjectType.PROCEDURE;
    }

    private String getDdl(ParserRuleContext ctx) {
        Token start = ctx.getStart();
        Token stop = ctx.getStop();
        CharStream stream = start.getTokenSource().getInputStream();
        return stream.getText(Interval.of(start.getStartIndex(), stop.getStopIndex()));
    }
}
