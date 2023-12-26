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
package com.oceanbase.odc.core.sql.parser;

import org.antlr.v4.runtime.tree.RuleNode;

import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_context_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_dblink_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_directory_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_function_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_index_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_outline_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_package_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_profile_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_resource_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_restore_point_stmtContext;
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
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;

import lombok.Getter;

@Getter
class OBOracleDropStatementVisitor extends OBParserBaseVisitor<DropStatement> {

    @Override
    public DropStatement visitChildren(RuleNode node) {
        return null;
    }

    @Override
    public DropStatement visitDrop_table_stmt(Drop_table_stmtContext ctx) {
        return new DropStatement(ctx, "TABLE");
    }

    @Override
    public DropStatement visitDrop_view_stmt(Drop_view_stmtContext ctx) {
        return new DropStatement(ctx, "VIEW");
    }

    @Override
    public DropStatement visitDrop_outline_stmt(Drop_outline_stmtContext ctx) {
        return new DropStatement(ctx, "OUTLINE");
    }

    @Override
    public DropStatement visitDrop_user_stmt(Drop_user_stmtContext ctx) {
        return new DropStatement(ctx, "USER");
    }

    @Override
    public DropStatement visitDrop_index_stmt(Drop_index_stmtContext ctx) {
        return new DropStatement(ctx, "INDEX");
    }

    @Override
    public DropStatement visitDrop_tenant_stmt(Drop_tenant_stmtContext ctx) {
        return new DropStatement(ctx, "TENANT");
    }

    @Override
    public DropStatement visitDrop_restore_point_stmt(Drop_restore_point_stmtContext ctx) {
        return new DropStatement(ctx, "RESTORE_POINT");
    }

    @Override
    public DropStatement visitDrop_resource_stmt(Drop_resource_stmtContext ctx) {
        return new DropStatement(ctx, "RESOURCE");
    }

    @Override
    public DropStatement visitDrop_tablegroup_stmt(Drop_tablegroup_stmtContext ctx) {
        return new DropStatement(ctx, "TABLEGROUP");
    }

    @Override
    public DropStatement visitDrop_synonym_stmt(Drop_synonym_stmtContext ctx) {
        return ctx.PUBLIC() != null ? new DropStatement(ctx, "PUBLIC SYNONYM") : new DropStatement(ctx, "SYNONYM");
    }

    @Override
    public DropStatement visitDrop_directory_stmt(Drop_directory_stmtContext ctx) {
        return new DropStatement(ctx, "DIRECTORY");
    }

    @Override
    public DropStatement visitDrop_tablespace_stmt(Drop_tablespace_stmtContext ctx) {
        return new DropStatement(ctx, "TABLESPACE");
    }

    @Override
    public DropStatement visitDrop_sequence_stmt(Drop_sequence_stmtContext ctx) {
        return new DropStatement(ctx, "SEQUENCE");
    }

    @Override
    public DropStatement visitDrop_dblink_stmt(Drop_dblink_stmtContext ctx) {
        return new DropStatement(ctx, "DBLINK");
    }

    @Override
    public DropStatement visitDrop_role_stmt(Drop_role_stmtContext ctx) {
        return new DropStatement(ctx, "ROLE");
    }

    @Override
    public DropStatement visitDrop_profile_stmt(Drop_profile_stmtContext ctx) {
        return new DropStatement(ctx, "PROFILE");
    }

    @Override
    public DropStatement visitDrop_package_stmt(Drop_package_stmtContext ctx) {
        return ctx.BODY() != null ? new DropStatement(ctx, "PACKAGE BODY") : new DropStatement(ctx, "PACKAGE");
    }

    @Override
    public DropStatement visitDrop_procedure_stmt(Drop_procedure_stmtContext ctx) {
        return new DropStatement(ctx, "PROCEDURE");
    }

    @Override
    public DropStatement visitDrop_function_stmt(Drop_function_stmtContext ctx) {
        return new DropStatement(ctx, "FUNCTION");
    }

    @Override
    public DropStatement visitDrop_trigger_stmt(Drop_trigger_stmtContext ctx) {
        return new DropStatement(ctx, "TRIGGER");
    }

    @Override
    public DropStatement visitDrop_context_stmt(Drop_context_stmtContext ctx) {
        return new DropStatement(ctx, "CONTEXT");
    }

    @Override
    public DropStatement visitDrop_type_stmt(Drop_type_stmtContext ctx) {
        return ctx.BODY() != null ? new DropStatement(ctx, "TYPE BODY") : new DropStatement(ctx, "TYPE");
    }

}
