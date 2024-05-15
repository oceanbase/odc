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
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleColumnGroupElementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_database_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_index_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_keystore_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_outline_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_profile_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_resource_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_sequence_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_session_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_system_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_tablegroup_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_tenant_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_user_profile_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_user_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_name_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_dblink_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_index_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_keystore_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_outline_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_profile_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_resource_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_role_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_savepoint_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_sequence_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_synonym_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_tablegroup_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_tablespace_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_tenant_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_user_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_view_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Database_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Delete_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_dblink_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_function_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_index_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_outline_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_package_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_profile_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_resource_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_role_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_sequence_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_synonym_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_tablegroup_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_tablespace_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_tenant_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_trigger_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_type_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_user_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_view_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Explain_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Fetch_next_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.For_updateContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Help_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Index_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Normal_relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Out_of_line_constraintContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.References_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Scope_or_scope_aliasContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_with_hierarchical_queryContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_charset_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_names_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_password_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_transaction_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Show_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Simple_selectContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Sys_var_and_valContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Truncate_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Update_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.User_with_host_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Var_and_valContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Variable_set_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseListener;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class OracleModeSqlParserListener extends OBParserBaseListener implements BasicParserListener {

    private boolean selectStmt;
    private boolean withForUpdate;
    private boolean fetchNext;
    private boolean whereClause;
    private boolean setStmt = false;

    private SqlType sqlType;
    private DBObjectType dbObjectType;
    private List<String> dbObjectNameList = new ArrayList<>();
    private List<DBIndex> indexes = new ArrayList<>();
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
    public void enterFor_update(For_updateContext ctx) {
        this.withForUpdate = true;
    }

    @Override
    public void enterFetch_next_clause(Fetch_next_clauseContext ctx) {
        this.fetchNext = true;
    }

    @Override
    public void enterSimple_select(Simple_selectContext ctx) {
        if (Objects.nonNull(ctx.WHERE())) {
            this.whereClause = true;
        }
    }

    @Override
    public void enterSelect_with_hierarchical_query(Select_with_hierarchical_queryContext ctx) {
        if (Objects.nonNull(ctx.WHERE())) {
            this.whereClause = true;
        }
    }

    @Override
    public void enterDrop_package_stmt(Drop_package_stmtContext ctx) {
        if (Objects.nonNull(ctx.BODY())) {
            this.dbObjectType = DBObjectType.PACKAGE_BODY;
        } else {
            this.dbObjectType = DBObjectType.PACKAGE;
        }
        this.sqlType = SqlType.DROP;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterDrop_procedure_stmt(Drop_procedure_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.PROCEDURE;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterDrop_function_stmt(Drop_function_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.FUNCTION;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterDrop_trigger_stmt(Drop_trigger_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.TRIGGER;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterDrop_type_stmt(Drop_type_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.TYPE;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

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
    public void enterHelp_stmt(Help_stmtContext ctx) {
        setSqlType(SqlType.HELP);
    }

    @Override
    public void enterShow_stmt(Show_stmtContext ctx) {
        setSqlType(SqlType.SHOW);
    }

    @Override
    public void enterDrop_table_stmt(Drop_table_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.TABLE;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterDrop_view_stmt(Drop_view_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.VIEW;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterDrop_outline_stmt(Drop_outline_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterDrop_user_stmt(Drop_user_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        for (User_with_host_nameContext user_with_host_nameContext : ctx.user_list().user_with_host_name()) {
            this.dbObjectNameList.add(handleObjectName(user_with_host_nameContext.user().getText()));
        }
    }

    @Override
    public void enterDrop_index_stmt(Drop_index_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        for (Relation_nameContext relation_nameContext : ctx.relation_name()) {
            this.dbObjectNameList.add(handleObjectName(relation_nameContext.getText()));
        }
    }

    @Override
    public void enterDrop_tenant_stmt(Drop_tenant_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_name().getText()));
    }

    @Override
    public void enterDrop_resource_stmt(Drop_resource_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_name().getText()));
    }

    @Override
    public void enterDrop_tablegroup_stmt(Drop_tablegroup_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_name().getText()));
    }

    @Override
    public void enterDrop_synonym_stmt(Drop_synonym_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.SYNONYM;
        this.dbObjectNameList.add(handleObjectName(ctx.synonym_name().getText()));
    }

    @Override
    public void enterDrop_tablespace_stmt(Drop_tablespace_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(handleObjectName(ctx.tablespace().getText()));
    }

    @Override
    public void enterDrop_sequence_stmt(Drop_sequence_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.SEQUENCE;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterDrop_dblink_stmt(Drop_dblink_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(handleObjectName(ctx.dblink().getText()));
    }

    @Override
    public void enterDrop_role_stmt(Drop_role_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(handleObjectName(ctx.role().getText()));
    }

    @Override
    public void enterDrop_profile_stmt(Drop_profile_stmtContext ctx) {
        this.sqlType = SqlType.DROP;
        this.dbObjectType = DBObjectType.OTHERS;
        this.dbObjectNameList.add(handleObjectName(ctx.profile_name().getText()));
    }

    @Override
    public void enterCreate_table_stmt(Create_table_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.TABLE;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterCreate_view_stmt(Create_view_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.VIEW;
        this.dbObjectNameList.add(handleObjectName(ctx.view_name().getText()));
    }

    @Override
    public void enterCreate_sequence_stmt(Create_sequence_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.SEQUENCE;
        this.dbObjectNameList.add(handleObjectName(ctx.relation_factor().getText()));
    }

    @Override
    public void enterCreate_user_stmt(Create_user_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.USER;
    }

    @Override
    public void enterCreate_index_stmt(Create_index_stmtContext ctx) {
        this.sqlType = SqlType.CREATE;
        this.dbObjectType = DBObjectType.INDEX;
        DBIndex index = new DBIndex();
        if (Objects.nonNull(ctx.normal_relation_factor().database_factor())) {
            index.setDatabaseName(handleObjectName(ctx.normal_relation_factor().database_factor().getText()));
        }
        if (Objects.nonNull(ctx.normal_relation_factor().relation_name())) {
            index.setName(handleObjectName(ctx.normal_relation_factor().relation_name().getText()));
        }
        index.setRange(DBIndexRangeType.GLOBAL);
        if (Objects.nonNull(ctx.opt_index_options())) {
            for (Index_optionContext index_optionContext : ctx.opt_index_options().index_option()) {
                if (Objects.nonNull(index_optionContext.LOCAL())) {
                    index.setRange(DBIndexRangeType.LOCAL);
                    break;
                }
            }
        }
        if (Objects.nonNull(ctx.with_column_group())) {
            List<DBColumnGroupElement> columnGroups = ctx.with_column_group()
                    .column_group_list().column_group_element().stream()
                    .map(c -> DBColumnGroupElement
                            .ofColumnGroupElement(new OracleColumnGroupElementFactory(c).generate()))
                    .collect(Collectors.toList());
            index.setColumnGroups(columnGroups);
        }
        indexes.add(index);
    }

    @Override
    public void enterTruncate_table_stmt(Truncate_table_stmtContext ctx) {
        setSqlType(SqlType.TRUNCATE);
        this.dbObjectType = DBObjectType.TABLE;
    }

    @Override
    public void enterOut_of_line_constraint(Out_of_line_constraintContext ctx) {
        References_clauseContext references_clauseContext = ctx.references_clause();
        if (references_clauseContext == null) {
            return;
        }
        Normal_relation_factorContext normal_relation_factorContext = references_clauseContext.normal_relation_factor();

        Database_factorContext database_factorContext = normal_relation_factorContext.database_factor();
        DBTableConstraint constraint = new DBTableConstraint();
        constraint.setType(DBConstraintType.FOREIGN_KEY);
        if (database_factorContext != null) {
            constraint.setReferenceSchemaName(handleObjectName(database_factorContext.getText()));
        }
        Relation_nameContext relation_nameContext = normal_relation_factorContext.relation_name();
        if (relation_nameContext != null) {
            constraint.setReferenceTableName(handleObjectName(relation_nameContext.getText()));
        }
        Column_name_listContext column_name_listContext = references_clauseContext.column_name_list();
        if (column_name_listContext != null) {
            List<Column_nameContext> column_nameContexts = column_name_listContext.column_name();
            constraint.setReferenceColumnNames(
                    column_nameContexts.stream().map(c -> handleObjectName(c.getText())).collect(Collectors.toList()));
        }
        this.foreignConstraint.add(constraint);
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
    }

    @Override
    public void enterAlter_index_stmt(Alter_index_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_system_stmt(Alter_system_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_user_stmt(Alter_user_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_user_profile_stmt(Alter_user_profile_stmtContext ctx) {
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
    public void enterAlter_keystore_stmt(Alter_keystore_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_sequence_stmt(Alter_sequence_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.SEQUENCE;
    }

    @Override
    public void enterAlter_session_stmt(Alter_session_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterAlter_profile_stmt(Alter_profile_stmtContext ctx) {
        setSqlType(SqlType.ALTER);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        if (Objects.isNull(sqlType)) {
            setSqlType(SqlType.OTHERS);
        }
    }

    private String handleObjectName(String name) {
        return StringUtils.unquoteOracleIdentifier(name);
    }

    @Override
    public void enterCreate_synonym_stmt(Create_synonym_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.SYNONYM;
    }

    @Override
    public void enterCreate_outline_stmt(Create_outline_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_tenant_stmt(Create_tenant_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_resource_stmt(Create_resource_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_tablegroup_stmt(Create_tablegroup_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_tablespace_stmt(Create_tablespace_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_keystore_stmt(Create_keystore_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_savepoint_stmt(Create_savepoint_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_dblink_stmt(Create_dblink_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_role_stmt(Create_role_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }

    @Override
    public void enterCreate_profile_stmt(Create_profile_stmtContext ctx) {
        setSqlType(SqlType.CREATE);
        this.dbObjectType = DBObjectType.OTHERS;
    }
}
