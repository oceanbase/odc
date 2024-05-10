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
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBIndex;
import com.oceanbase.tools.dbbrowser.model.DBIndexRangeType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.util.StringUtils;
import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLTableElementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_cluster_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_outline_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_resource_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_system_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_tablegroup_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_tablespace_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_tenant_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_function_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_view_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Delete_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_function_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_index_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_outline_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_resource_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_tablegroup_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_tablespace_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_tenant_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_trigger_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_user_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_view_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Explain_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Help_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Limit_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.No_table_select_with_order_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Opt_for_update_waitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Scope_or_scope_aliasContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_clause_set_with_order_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_with_parens_with_order_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Set_charset_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Set_names_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Set_password_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Set_transaction_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Show_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Simple_select_with_order_and_limitContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Sys_var_and_valContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_elementContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Truncate_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Update_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Use_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.User_with_host_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Var_and_valContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Variable_set_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseListener;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class MysqlModeSqlParserListener extends OBParserBaseListener implements BasicParserListener {

    private boolean selectStmt;
    private boolean withForUpdate;
    private boolean limitClause;
    private SqlType sqlType;
    private DBObjectType dbObjectType;
    private boolean setStmt = false;
    private List<String> dbObjectNameList = new ArrayList<>();
    private List<DBIndex> indexes = new ArrayList<>();
    private List<ColumnDefinition> columns = new ArrayList<>();
    private List<DBTableConstraint> foreignConstraint = new ArrayList<>();

    private void setSqlType(SqlType newType) {
        // TODO: may need some judgement if some type can not be overrided
        if (Objects.isNull(this.sqlType) || SqlType.OTHERS == this.sqlType) {
            this.sqlType = newType;
        }
    }

    private void setDbObjectType(@NonNull DBObjectType objectType) {
        if (this.dbObjectType == null) {
            this.dbObjectType = objectType;
        } else if (!this.dbObjectType.equals(objectType)) {
            this.dbObjectType = DBObjectType.OTHERS;
        }
    }

    @Override
    public void enterSelect_stmt(Select_stmtContext ctx) {
        setSqlType(SqlType.SELECT);
        if (ctx.getParent() instanceof OBParser.StmtContext) {
            this.selectStmt = true;
        }
    }

    @Override
    public void enterOpt_for_update_wait(Opt_for_update_waitContext ctx) {
        this.withForUpdate = true;
    }

    @Override
    public void enterLimit_clause(Limit_clauseContext ctx) {
        this.limitClause = true;
    }

    /**
     * Keep the grammer nodes for entering limit clause for future refactor about limit judgement
     *
     * @param ctx
     */
    @Override
    public void enterSelect_clause_set_with_order_and_limit(Select_clause_set_with_order_and_limitContext ctx) {}

    @Override
    public void enterNo_table_select_with_order_and_limit(No_table_select_with_order_and_limitContext ctx) {}

    @Override
    public void enterSimple_select_with_order_and_limit(Simple_select_with_order_and_limitContext ctx) {}

    @Override
    public void enterSelect_with_parens_with_order_and_limit(Select_with_parens_with_order_and_limitContext ctx) {}

    @Override
    public void enterDelete_stmt(Delete_stmtContext ctx) {
        setSqlType(SqlType.DELETE);
    }

    @Override
    public void enterInsert_stmt(Insert_stmtContext ctx) {
        setSqlType(SqlType.INSERT);
    }

    @Override
    public void enterVariable_set_stmt(Variable_set_stmtContext ctx) {
        setSqlType(SqlType.SET);
        this.setStmt = true;
    }

    @Override
    public void exitVariable_set_stmt(Variable_set_stmtContext ctx) {
        this.setStmt = false;
    }

    @Override
    public void enterVar_and_val(Var_and_valContext ctx) {
        if (!this.setStmt) {
            return;
        }
        ParseTree parseTree = ctx.getChild(0);
        if (!(parseTree instanceof TerminalNode)) {
            return;
        }
        /**
         * 终结节点，说明是 {@code USER_VARIABLE} 或 {@code SYSTEM_VARIABLE}
         */
        Token variableToken = ((TerminalNode) parseTree).getSymbol();
        if (variableToken.getType() == OBLexer.USER_VARIABLE) {
            setDbObjectType(DBObjectType.USER_VARIABLE);
        } else if (variableToken.getType() == OBLexer.SYSTEM_VARIABLE) {
            setDbObjectType(DBObjectType.SYSTEM_VARIABLE);
        }
    }

    @Override
    public void enterSys_var_and_val(Sys_var_and_valContext ctx) {
        if (!this.setStmt) {
            return;
        }
        setDbObjectType(DBObjectType.SYSTEM_VARIABLE);
    }

    @Override
    public void enterScope_or_scope_alias(Scope_or_scope_aliasContext ctx) {
        if (!this.setStmt) {
            return;
        }
        ParseTree parseTree = ctx.getChild(0);
        if (!(parseTree instanceof TerminalNode)) {
            return;
        }
        Token scopeToken = ((TerminalNode) parseTree).getSymbol();
        if (scopeToken.getType() == OBLexer.GLOBAL || scopeToken.getType() == OBLexer.GLOBAL_ALIAS) {
            setDbObjectType(DBObjectType.GLOBAL_VARIABLE);
        } else if (scopeToken.getType() == OBLexer.SESSION || scopeToken.getType() == OBLexer.SESSION_ALIAS) {
            setDbObjectType(DBObjectType.SESSION_VARIABLE);
        }
    }

    @Override
    public void enterSet_password_stmt(Set_password_stmtContext ctx) {
        setSqlType(SqlType.SET);
    }

    @Override
    public void enterSet_names_stmt(Set_names_stmtContext ctx) {
        setSqlType(SqlType.SET);
    }

    @Override
    public void enterSet_charset_stmt(Set_charset_stmtContext ctx) {
        setSqlType(SqlType.SET);
    }

    @Override
    public void enterSet_transaction_stmt(Set_transaction_stmtContext ctx) {
        setSqlType(SqlType.SET);
    }

    @Override
    public void enterUpdate_stmt(Update_stmtContext ctx) {
        setSqlType(SqlType.UPDATE);
    }

    @Override
    public void enterExplain_stmt(Explain_stmtContext ctx) {
        if (ctx.explain_or_desc().EXPLAIN() != null) {
            setSqlType(SqlType.EXPLAIN);
        } else {
            setSqlType(SqlType.DESC);
        }
    }

    // TODO: extract table name into ObDbTableNames

    @Override
    public void enterUse_database_stmt(Use_database_stmtContext ctx) {
        setSqlType(SqlType.USE_DB);
    }

    @Override
    public void enterHelp_stmt(Help_stmtContext ctx) {
        setSqlType(SqlType.HELP);
    }

    @Override
    public void enterShow_stmt(Show_stmtContext ctx) {
        setSqlType(SqlType.SHOW);
    }

    @Override
    public void enterDrop_database_stmt(Drop_database_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.DATABASE;
        this.dbObjectNameList.add(ctx.database_factor().getText());
    }

    @Override
    public void enterDrop_table_stmt(Drop_table_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.TABLE;
        for (Relation_factorContext relation_factorContext : ctx.table_list().relation_factor()) {
            this.dbObjectNameList.add(relation_factorContext.getText());
        }
    }

    @Override
    public void enterDrop_view_stmt(Drop_view_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.VIEW;
        for (Relation_factorContext relation_factorContext : ctx.table_list().relation_factor()) {
            this.dbObjectNameList.add(relation_factorContext.getText());
        }
    }

    @Override
    public void enterDrop_function_stmt(Drop_function_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.FUNCTION;
        this.dbObjectNameList.add(ctx.relation_factor().getText());
    }

    @Override
    public void enterDrop_procedure_stmt(Drop_procedure_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.PROCEDURE;
        this.dbObjectNameList.add(ctx.relation_factor().getText());
    }

    @Override
    public void enterDrop_trigger_stmt(Drop_trigger_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.TRIGGER;
        this.dbObjectNameList.add(ctx.relation_factor().getText());
    }

    @Override
    public void enterDrop_outline_stmt(Drop_outline_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(ctx.relation_factor().getText());
    }

    @Override
    public void enterDrop_user_stmt(Drop_user_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        for (User_with_host_nameContext user_with_host_nameContext : ctx.user_list().user_with_host_name()) {
            this.dbObjectNameList.add(user_with_host_nameContext.user().getText());
        }
    }

    @Override
    public void enterDrop_index_stmt(Drop_index_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(ctx.relation_name().getText());
    }

    @Override
    public void enterDrop_tenant_stmt(Drop_tenant_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(ctx.relation_name().getText());
    }

    @Override
    public void enterDrop_resource_stmt(Drop_resource_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(ctx.relation_name().getText());
    }

    @Override
    public void enterDrop_tablegroup_stmt(Drop_tablegroup_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(ctx.relation_name().getText());
    }

    @Override
    public void enterDrop_tablespace_stmt(Drop_tablespace_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(ctx.tablespace().getText());
    }

    @Override
    public void enterCreate_database_stmt(Create_database_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.DATABASE;
        this.dbObjectNameList.add(ctx.database_factor().getText());
    }

    @Override
    public void enterCreate_table_stmt(Create_table_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.TABLE;
        this.dbObjectNameList.add(ctx.relation_factor().getText());
        for (OBParser.Table_elementContext element : ctx.table_element_list().table_element()) {
            if (Objects.nonNull(element.column_definition())) {
                columns.add(ColumnDefinition.builder()
                        .name(handleObjectName(
                                element.column_definition().column_definition_ref().column_name().getText()))
                        .isStored(Objects.nonNull(element.column_definition().STORED())).build());
            }
        }
    }

    @Override
    public void enterCreate_view_stmt(Create_view_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.VIEW;
        this.dbObjectNameList.add(ctx.view_name().getText());
    }

    @Override
    public void enterCreate_function_stmt(Create_function_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.FUNCTION;
        this.dbObjectNameList.add(ctx.NAME_OB().getText());
    }

    @Override
    public void enterTruncate_table_stmt(Truncate_table_stmtContext ctx) {
        setSqlType(SqlType.TRUNCATE);
        this.dbObjectType = DBObjectType.TABLE;
    }

    @Override
    public void enterAlter_database_stmt(Alter_database_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.DATABASE;
    }

    @Override
    public void enterAlter_outline_stmt(Alter_outline_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_table_stmt(Alter_table_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.TABLE;
        this.dbObjectNameList.add(ctx.relation_factor().getText());
    }

    @Override
    public void enterAlter_system_stmt(Alter_system_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_tablespace_stmt(Alter_tablespace_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_tenant_stmt(Alter_tenant_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_resource_stmt(Alter_resource_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_tablegroup_stmt(Alter_tablegroup_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_cluster_stmt(Alter_cluster_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        if (Objects.isNull(sqlType)) {
            setSqlType(SqlType.OTHERS);
        }
    }

    @Override
    public void enterTable_element(Table_elementContext ctx) {
        StatementFactory<TableElement> factory = new MySQLTableElementFactory(ctx);
        TableElement elt = factory.generate();
        if (elt instanceof OutOfLineIndex) {
            OutOfLineIndex outOfLineIndex = (OutOfLineIndex) elt;
            DBIndex index = new DBIndex();
            index.setName(handleObjectName(outOfLineIndex.getIndexName()));
            index.setRange(DBIndexRangeType.GLOBAL);
            if (outOfLineIndex.getIndexOptions() != null && outOfLineIndex.getIndexOptions().getGlobal() != null
                    && !outOfLineIndex.getIndexOptions().getGlobal()) {
                index.setRange(DBIndexRangeType.LOCAL);
            }
            if (outOfLineIndex.getColumnGroupElements() != null) {
                index.setColumnGroups(outOfLineIndex.getColumnGroupElements()
                        .stream().map(DBColumnGroupElement::ofColumnGroupElement).collect(Collectors.toList()));
            }
            indexes.add(index);
        } else if (elt instanceof OutOfLineConstraint) {
            OutOfLineConstraint outOfLineConstraint = (OutOfLineConstraint) elt;
            if (outOfLineConstraint.isUniqueKey()) {
                DBIndex index = new DBIndex();
                index.setName(handleObjectName(outOfLineConstraint.getIndexName()));
                index.setRange(DBIndexRangeType.GLOBAL);
                if (outOfLineConstraint.getState() != null && outOfLineConstraint.getState().getIndexOptions() != null
                        && outOfLineConstraint.getState().getIndexOptions().getGlobal() != null
                        && !outOfLineConstraint.getState().getIndexOptions().getGlobal()) {
                    index.setRange(DBIndexRangeType.LOCAL);
                }
                if (outOfLineConstraint.getColumnGroupElements() != null) {
                    index.setColumnGroups(outOfLineConstraint.getColumnGroupElements()
                            .stream().map(DBColumnGroupElement::ofColumnGroupElement).collect(Collectors.toList()));
                }
                indexes.add(index);
            } else if (elt instanceof OutOfLineForeignConstraint) {
                OutOfLineForeignConstraint foreign = (OutOfLineForeignConstraint) elt;
                DBTableConstraint constraint = new DBTableConstraint();
                constraint.setType(DBConstraintType.FOREIGN_KEY);
                constraint.setReferenceTableName(handleObjectName(foreign.getReference().getRelation()));
                constraint.setReferenceSchemaName(handleObjectName(foreign.getReference().getSchema()));
                foreignConstraint.add(constraint);
            }
        }
    }

    private String handleObjectName(String name) {
        return StringUtils.unquoteMySqlIdentifier(name);
    }

    @Data
    @Builder
    public static class ColumnDefinition {
        private String name;
        private Boolean isStored;
    }
}
