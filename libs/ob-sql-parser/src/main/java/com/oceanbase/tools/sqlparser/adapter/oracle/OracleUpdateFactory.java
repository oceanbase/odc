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
package com.oceanbase.tools.sqlparser.adapter.oracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_definition_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Dml_table_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Dml_table_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Expr_or_defaultContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Normal_asgn_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_where_extensionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Update_asgn_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Update_asgn_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Update_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.update.Update;
import com.oceanbase.tools.sqlparser.statement.update.UpdateAssign;

import lombok.NonNull;

/**
 * {@link OracleUpdateFactory}
 *
 * @author yh263208
 * @date 2022-12-20 15:59
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleUpdateFactory extends OBParserBaseVisitor<Update> implements StatementFactory<Update> {

    private final Update_stmtContext updateStmtContext;

    public OracleUpdateFactory(@NonNull Update_stmtContext updateStmtContext) {
        this.updateStmtContext = updateStmtContext;
    }

    @Override
    public Update generate() {
        return visit(this.updateStmtContext);
    }

    @Override
    public Update visitUpdate_stmt(Update_stmtContext ctx) {
        Update update = new Update(ctx, Arrays.asList(visitDmlTableClauseContext(ctx.dml_table_clause())),
                visitUpdateAsgnList(ctx.update_asgn_list()));

        Opt_where_extensionContext whereExtension = ctx.opt_where_extension();
        if (whereExtension.opt_where() == null) {
            StatementFactory<Expression> factory = new OracleExpressionFactory(whereExtension.obj_access_ref());
            update.setCursor(true);
            update.setWhere(factory.generate());
        } else if (whereExtension.opt_where().expr() != null) {
            StatementFactory<Expression> factory = new OracleExpressionFactory(whereExtension.opt_where().expr());
            update.setCursor(false);
            update.setWhere(factory.generate());
        }
        return update;
    }

    private FromReference visitDmlTableClauseContext(Dml_table_clauseContext ctx) {
        String alias = null;
        if (ctx.relation_name() != null) {
            alias = ctx.relation_name().getText();
        }
        if (ctx.dml_table_name() != null) {
            Dml_table_nameContext dmlTableName = ctx.dml_table_name();
            Relation_factorContext r = dmlTableName.relation_factor();
            PartitionUsage partitionUsage = null;
            if (dmlTableName.use_partition() != null) {
                OraclePartitonUsageFactory factory =
                        new OraclePartitonUsageFactory(dmlTableName.use_partition());
                partitionUsage = factory.generate();
            }
            NameReference nameReference = new NameReference(ctx, OracleFromReferenceFactory.getSchemaName(r),
                    OracleFromReferenceFactory.getRelation(r), alias);
            if (partitionUsage != null) {
                nameReference.setPartitionUsage(partitionUsage);
            }
            nameReference.setUserVariable(OracleFromReferenceFactory.getUserVariable(r));
            return nameReference;
        } else if (ctx.select_with_parens() != null) {
            OracleSelectBodyFactory factory = new OracleSelectBodyFactory(ctx.select_with_parens());
            SelectBody selectBody = factory.generate();
            return new ExpressionReference(ctx, selectBody, alias);
        } else {
            OracleSelectBodyFactory factory = new OracleSelectBodyFactory(ctx.subquery());
            SelectBody select = factory.generate();
            if (ctx.order_by() != null) {
                OracleOrderByFactory orderByFactory = new OracleOrderByFactory(ctx.order_by());
                select.setOrderBy(orderByFactory.generate());
            }
            if (ctx.fetch_next_clause() != null) {
                OracleFetchFactory fetchFactory = new OracleFetchFactory(ctx.fetch_next_clause());
                select.setFetch(fetchFactory.generate());
            }
            return new ExpressionReference(ctx, select, alias);
        }
    }

    private List<UpdateAssign> visitUpdateAsgnList(Update_asgn_listContext ctx) {
        List<UpdateAssign> returnVal = new ArrayList<>();
        if (ctx.normal_asgn_list() == null) {
            return returnVal;
        }
        Normal_asgn_listContext normalAsgnList = ctx.normal_asgn_list();
        for (Update_asgn_factorContext updateAsgnFactor : normalAsgnList.update_asgn_factor()) {
            List<ColumnReference> columnList = new ArrayList<>();
            Expression expression = null;
            boolean useDefault = false;

            if (updateAsgnFactor.column_definition_ref() != null) {
                OracleColumnRefFactory factory =
                        new OracleColumnRefFactory(updateAsgnFactor.column_definition_ref());
                ColumnReference columnReference = factory.generate();
                columnList = Arrays.asList(columnReference);
            }
            if (updateAsgnFactor.expr_or_default() != null) {
                Expr_or_defaultContext exprOrDefault = updateAsgnFactor.expr_or_default();
                if (exprOrDefault.bit_expr() != null) {
                    OracleExpressionFactory factory = new OracleExpressionFactory(exprOrDefault.bit_expr());
                    expression = factory.generate();
                }
                if (exprOrDefault.DEFAULT() != null) {
                    useDefault = true;
                }
            }
            if (updateAsgnFactor.column_list() != null) {
                for (Column_definition_refContext column : updateAsgnFactor.column_list()
                        .column_definition_ref()) {
                    OracleColumnRefFactory factory = new OracleColumnRefFactory(column);
                    columnList.add(factory.generate());
                }
            }
            if (updateAsgnFactor.subquery() != null) {
                OracleSelectBodyFactory factory = new OracleSelectBodyFactory(updateAsgnFactor.subquery());
                expression = factory.generate();
            }
            returnVal.add(new UpdateAssign(updateAsgnFactor, columnList, expression, useDefault));
        }
        return returnVal;
    }

}
