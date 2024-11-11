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

import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBPLType;
import com.oceanbase.tools.dbbrowser.model.DBPLVariable;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.util.StringUtils;
import com.oceanbase.tools.sqlparser.obmysql.PLParser;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Create_function_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Create_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Drop_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Sp_declContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Sp_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParserBaseListener;

import lombok.Getter;

@Getter
public class MysqlModePLParserListener extends PLParserBaseListener implements BasicParserListener {

    private List<DBPLParam> paramList = new ArrayList<>(); // pl参数
    private List<DBPLVariable> varibaleList = new ArrayList<>(); // pl定义的变量
    private List<DBPLType> typeList = new ArrayList<>();// pl定义的类型
    private String returnType; // function返回值
    private String plName; // pl对象名
    private String plType;// pl类型，function、procedure、package

    private SqlType sqlType;
    private DBObjectType dbObjectType;
    private List<String> dbObjectNameList = new ArrayList<>();

    public boolean isEmpty() {
        return paramList.isEmpty() && varibaleList.isEmpty()
                && typeList.isEmpty() && StringUtils.isEmpty(returnType)
                && StringUtils.isEmpty(plName) && StringUtils.isEmpty(plType);
    }

    @Override
    public void enterSp_name(Sp_nameContext ctx) {
        this.plName = ctx.getText();
        List<ParseTree> children = ctx.getParent().children;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getText().equalsIgnoreCase(this.plName)) {
                this.plType = children.get(i - 1).getText();
                break;
            }
        }
    }

    @Override
    public void enterCreate_procedure_stmt(Create_procedure_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.PROCEDURE;
        this.dbObjectNameList.add(ctx.sp_name().getText());

        for (int j = 0; j < ctx.getChildCount(); j++) {
            if (ctx.getChild(j) instanceof PLParser.Sp_param_listContext) {
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
                            String paramName = paramContext.ident().getText();
                            plParam.setParamName(StringUtils.unquoteMySqlIdentifier(paramName));
                            plParam.setSeqNum(++paramCount);
                            plParam.setDataType(paramContext.param_type().getText());
                            if (paramContext.IN() != null) {
                                plParam.setParamMode(DBPLParamMode.IN);
                            } else if (paramContext.OUT() != null) {
                                plParam.setParamMode(DBPLParamMode.OUT);
                            } else if (paramContext.INOUT() != null) {
                                plParam.setParamMode(DBPLParamMode.INOUT);
                            }
                            paramList.add(plParam);
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public void enterDrop_procedure_stmt(Drop_procedure_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.PROCEDURE;
        this.dbObjectNameList.add(ctx.sp_name().getText());
    }

    @Override
    public void enterCreate_function_stmt(Create_function_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.FUNCTION;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i).getText().equalsIgnoreCase("returns")) {
                this.returnType = ctx.getChild(i + 1).getText();
                break;
            }
        }

        for (int j = 0; j < ctx.getChildCount(); j++) {
            if (ctx.getChild(j) instanceof PLParser.Sp_fparam_listContext) {

                PLParser.Sp_fparam_listContext paramlist = (PLParser.Sp_fparam_listContext) ctx.getChild(j);
                // function param expression count
                int count = paramlist.getChildCount();
                // function param count
                int paramCount = 0;
                for (int i = 0; i < count; i++) {
                    if (paramlist.getChild(i) instanceof PLParser.Sp_fparamContext) {
                        PLParser.Sp_fparamContext paramContext = (PLParser.Sp_fparamContext) paramlist.getChild(i);
                        DBPLParam plParam = new DBPLParam();
                        String paramName = paramContext.ident().getText();
                        plParam.setParamName(StringUtils.unquoteMySqlIdentifier(paramName));
                        plParam.setSeqNum(++paramCount);
                        plParam.setDataType(paramContext.param_type().getText());
                        plParam.setParamMode(DBPLParamMode.IN);
                        paramList.add(plParam);
                    }
                }
                break;
            }
        }
    }

    @Override
    public void enterSp_decl(Sp_declContext ctx) {
        String var = ctx.getChild(1).getText();
        String type = ctx.getChild(2).getText();
        DBPLVariable varibale = new DBPLVariable();
        varibale.setVarName(var);
        varibale.setVarType(type);
        this.varibaleList.add(varibale);
    }

    @Override
    public void enterCall_sp_stmt(PLParser.Call_sp_stmtContext ctx) {
        this.sqlType = SqlType.CALL;
        this.dbObjectType = DBObjectType.PROCEDURE;
    }

}
