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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Bit_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_attributeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_definitionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_definition_opt_datatypeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_definition_refContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Constraint_stateContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Cur_timestamp_funcContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Cur_timestamp_func_paramsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Data_typeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Generated_column_attributeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Index_using_algorithmContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Normal_relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Now_or_signed_literalContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_column_attribute_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_generated_column_attribute_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_generated_option_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_identity_attributeContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_index_optionsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_skip_index_type_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Out_of_line_constraintContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Out_of_line_indexContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Out_of_line_index_stateContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Out_of_line_primary_indexContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Out_of_line_unique_indexContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Reference_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.References_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Sequence_option_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Signed_literal_paramsContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Table_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Visibility_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.ForeignReference;
import com.oceanbase.tools.sqlparser.statement.createtable.ForeignReference.OnOption;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption.Type;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionParam;
import com.oceanbase.tools.sqlparser.statement.sequence.SequenceOptions;

import lombok.NonNull;

/**
 * {@link OracleTableElementFactory}
 *
 * @author yh263208
 * @date 2023-05-23 10:24
 * @since ODC_release_4.2.0
 * @see StatementFactory
 */
public class OracleTableElementFactory extends OBParserBaseVisitor<TableElement>
        implements StatementFactory<TableElement> {

    private final ParserRuleContext parserRuleContext;

    public OracleTableElementFactory(@NonNull Table_elementContext tableElementContext) {
        this.parserRuleContext = tableElementContext;
    }

    public OracleTableElementFactory(@NonNull Column_definition_opt_datatypeContext parserRuleContext) {
        this.parserRuleContext = parserRuleContext;
    }

    public OracleTableElementFactory(@NonNull Column_definitionContext parserRuleContext) {
        this.parserRuleContext = parserRuleContext;
    }

    public OracleTableElementFactory(@NonNull Out_of_line_constraintContext parserRuleContext) {
        this.parserRuleContext = parserRuleContext;
    }

    public OracleTableElementFactory(@NonNull Out_of_line_primary_indexContext parserRuleContext) {
        this.parserRuleContext = parserRuleContext;
    }

    @Override
    public TableElement generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public TableElement visitOut_of_line_constraint(Out_of_line_constraintContext ctx) {
        String constraintName = ctx.constraint_and_name() == null
                ? null
                : ctx.constraint_and_name().constraint_name().getText();
        OutOfLineConstraint constraint;
        if (ctx.out_of_line_unique_index() != null) {
            constraint = new OutOfLineConstraint(ctx, (OutOfLineConstraint) visit(ctx.out_of_line_unique_index()));
        } else if (ctx.out_of_line_primary_index() != null) {
            constraint = new OutOfLineConstraint(ctx, (OutOfLineConstraint) visit(ctx.out_of_line_primary_index()));
        } else if (ctx.FOREIGN() != null) {
            List<SortColumn> columns = ctx.column_name_list().column_name().stream()
                    .map(c -> new SortColumn(c, new ColumnReference(c, null, null, c.getText())))
                    .collect(Collectors.toList());
            ConstraintState state = visitConstraintState(ctx.constraint_state());
            constraint = new OutOfLineForeignConstraint(ctx, state,
                    columns, visitForeignReference(ctx.references_clause()));
        } else {
            Expression checkExpr = new OracleExpressionFactory(ctx.expr()).generate();
            ConstraintState state = visitConstraintState(ctx.constraint_state());
            constraint = new OutOfLineCheckConstraint(ctx, state, checkExpr);
        }
        constraint.setConstraintName(constraintName);
        return constraint;
    }

    @Override
    public TableElement visitOut_of_line_primary_index(Out_of_line_primary_indexContext ctx) {
        List<SortColumn> columns = ctx.column_name_list().column_name().stream()
                .map(c -> new SortColumn(c, new ColumnReference(c, null, null, c.getText())))
                .collect(Collectors.toList());
        ConstraintState state = visitConstraintState(ctx.out_of_line_index_state());
        OutOfLineConstraint outOfLineConstraint = new OutOfLineConstraint(ctx, state, columns);
        outOfLineConstraint.setPrimaryKey(true);
        return outOfLineConstraint;
    }

    @Override
    public TableElement visitOut_of_line_unique_index(Out_of_line_unique_indexContext ctx) {
        List<SortColumn> columns = ctx.sort_column_list().sort_column_key().stream()
                .map(c -> new OracleSortColumnFactory(c).generate()).collect(Collectors.toList());
        ConstraintState state = visitConstraintState(ctx.out_of_line_index_state());
        OutOfLineConstraint outOfLineConstraint = new OutOfLineConstraint(ctx, state, columns);
        outOfLineConstraint.setUniqueKey(true);
        return outOfLineConstraint;
    }

    @Override
    public TableElement visitOut_of_line_index(Out_of_line_indexContext ctx) {
        String name = ctx.index_name() == null ? null : ctx.index_name().getText();
        List<SortColumn> columns = ctx.sort_column_list().sort_column_key().stream()
                .map(c -> new OracleSortColumnFactory(c).generate()).collect(Collectors.toList());
        OutOfLineIndex index = new OutOfLineIndex(ctx, name, columns);
        index.setIndexOptions(getIndexOptions(ctx.index_using_algorithm(), ctx.opt_index_options()));
        return index;
    }

    @Override
    public TableElement visitColumn_definition(Column_definitionContext ctx) {
        return getColumnDefinition(new ColumnContextProvider() {
            @Override
            public ParserRuleContext getSelf() {
                return ctx;
            }

            @Override
            public Data_typeContext getDataTypeCtx() {
                return ctx.data_type();
            }

            @Override
            public Column_definition_refContext getColumnDefinitionRefCtx() {
                return ctx.column_definition_ref();
            }

            @Override
            public Visibility_optionContext getVisibilityOptionCtx() {
                return ctx.visibility_option();
            }

            @Override
            public TerminalNode getAsNode() {
                return ctx.AS();
            }

            @Override
            public Opt_identity_attributeContext getOptIdentityAttributeCtx() {
                return ctx.opt_identity_attribute();
            }

            @Override
            public Sequence_option_listContext getSequenceOptionListCtx() {
                return ctx.sequence_option_list();
            }

            @Override
            public Bit_exprContext getBitExprCtx() {
                return ctx.bit_expr();
            }

            @Override
            public Opt_generated_option_listContext getOptGeneratedOptionListCtx() {
                return ctx.opt_generated_option_list();
            }

            @Override
            public TerminalNode getVirtualNode() {
                return ctx.VIRTUAL();
            }

            @Override
            public Opt_column_attribute_listContext getOptColumnAttributeListCtx() {
                return ctx.opt_column_attribute_list();
            }

            @Override
            public Opt_generated_column_attribute_listContext getOptGeneratedColumnAttributeListCtx() {
                return ctx.opt_generated_column_attribute_list();
            }
        });
    }

    @Override
    public TableElement visitColumn_definition_opt_datatype(Column_definition_opt_datatypeContext ctx) {
        return getColumnDefinition(new ColumnContextProvider() {
            @Override
            public ParserRuleContext getSelf() {
                return ctx;
            }

            @Override
            public Data_typeContext getDataTypeCtx() {
                return ctx.data_type();
            }

            @Override
            public Column_definition_refContext getColumnDefinitionRefCtx() {
                return ctx.column_definition_ref();
            }

            @Override
            public Visibility_optionContext getVisibilityOptionCtx() {
                return ctx.visibility_option();
            }

            @Override
            public TerminalNode getAsNode() {
                return ctx.AS();
            }

            @Override
            public Opt_identity_attributeContext getOptIdentityAttributeCtx() {
                return ctx.opt_identity_attribute();
            }

            @Override
            public Sequence_option_listContext getSequenceOptionListCtx() {
                return ctx.sequence_option_list();
            }

            @Override
            public Bit_exprContext getBitExprCtx() {
                return ctx.bit_expr();
            }

            @Override
            public Opt_generated_option_listContext getOptGeneratedOptionListCtx() {
                return ctx.opt_generated_option_list();
            }

            @Override
            public TerminalNode getVirtualNode() {
                return ctx.VIRTUAL();
            }

            @Override
            public Opt_column_attribute_listContext getOptColumnAttributeListCtx() {
                return ctx.opt_column_attribute_list();
            }

            @Override
            public Opt_generated_column_attribute_listContext getOptGeneratedColumnAttributeListCtx() {
                return ctx.opt_generated_column_attribute_list();
            }
        });
    }

    private ColumnDefinition getColumnDefinition(ColumnContextProvider ctx) {
        DataType dataType = null;
        if (ctx.getDataTypeCtx() != null) {
            dataType = new OracleDataTypeFactory(ctx.getDataTypeCtx()).generate();
        }
        StatementFactory<ColumnReference> factory = new OracleColumnRefFactory(ctx.getColumnDefinitionRefCtx());
        ColumnDefinition definition = new ColumnDefinition(ctx.getSelf(), factory.generate(), dataType);
        if (ctx.getVisibilityOptionCtx() != null) {
            definition.setVisible(ctx.getVisibilityOptionCtx().INVISIBLE() == null);
        }
        if (ctx.getAsNode() != null) {
            // generated column
            GenerateOption generateOption;
            if (ctx.getOptIdentityAttributeCtx() != null) {
                SequenceOptions options = null;
                if (ctx.getSequenceOptionListCtx() != null) {
                    options = new OracleSequenceOptionsFactory(ctx.getSequenceOptionListCtx()).generate();
                }
                generateOption = new GenerateOption(ctx.getSelf(), options);
            } else {
                StatementFactory<Expression> exprFactory = new OracleExpressionFactory(ctx.getBitExprCtx());
                generateOption = new GenerateOption(ctx.getSelf(), exprFactory.generate());
            }
            if (ctx.getOptGeneratedOptionListCtx() != null) {
                Opt_generated_option_listContext c = ctx.getOptGeneratedOptionListCtx();
                CharStream input = c.getStart().getInputStream();
                String str = input.getText(Interval.of(c.getStart().getStartIndex(), c.getStop().getStopIndex()));
                generateOption.setGenerateOption(StringUtils.isEmpty(str) ? null : str);
            }
            if (ctx.getVirtualNode() != null) {
                generateOption.setType(Type.VIRTUAL);
            }
            definition.setGenerateOption(generateOption);
        }
        if (ctx.getOptColumnAttributeListCtx() != null) {
            ColumnAttributes attributes = visitColumnAttributeList(ctx.getOptColumnAttributeListCtx());
            definition.setColumnAttributes(attributes);
        } else if (ctx.getOptGeneratedColumnAttributeListCtx() != null) {
            ColumnAttributes attributes =
                    visitGeneratedColumnAttributeList(ctx.getOptGeneratedColumnAttributeListCtx());
            definition.setColumnAttributes(attributes);
        }
        return definition;
    }

    private ConstraintState visitConstraintState(Out_of_line_index_stateContext context) {
        if (context == null) {
            return null;
        }
        ConstraintState state = new ConstraintState(context);
        if (context.USING() != null && context.INDEX() != null) {
            state.setUsingIndexFlag(true);
        }
        if (context.opt_index_options() != null) {
            state.setIndexOptions(new OracleIndexOptionsFactory(context.opt_index_options()).generate());
        }
        return state;
    }

    private ColumnAttributes visitColumnAttributeList(Opt_column_attribute_listContext context) {
        ColumnAttributes attributes = new ColumnAttributes(context);
        if (context.opt_column_attribute_list() != null) {
            attributes.merge(visitColumnAttributeList(context.opt_column_attribute_list()));
        }
        attributes.merge(visitColumnAttribute(context.column_attribute()));
        return attributes;
    }

    private ColumnAttributes visitColumnAttribute(Column_attributeContext ctx) {
        ColumnAttributes attributes = new ColumnAttributes(ctx);
        if (ctx.DEFAULT() != null) {
            attributes.setDefaultValue(new OracleExpressionFactory(ctx.bit_expr()).generate());
        } else if (ctx.ORIG_DEFAULT() != null) {
            attributes.setOrigDefault(visitNowOrSignedLiteral(ctx.now_or_signed_literal()));
        } else if (ctx.ID() != null) {
            attributes.setId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.SKIP_INDEX() != null) {
            List<String> skipIndexTypes = new ArrayList<>();
            if (ctx.opt_skip_index_type_list() != null) {
                getSkipIndexTypes(ctx.opt_skip_index_type_list(), skipIndexTypes);
            }
            if (ctx.skip_index_type() != null) {
                skipIndexTypes.add(ctx.skip_index_type().getText());
            }
            attributes.setSkipIndexTypes(skipIndexTypes);
        } else {
            String name = null;
            if (ctx.constraint_and_name() != null) {
                name = ctx.constraint_and_name().constraint_name().getText();
            }
            ConstraintState state = visitConstraintState(ctx.constraint_state());
            if (ctx.CHECK() == null && ctx.references_clause() == null) {
                InLineConstraint attribute = new InLineConstraint(ctx, name, state);
                if (ctx.NULLX() != null) {
                    attribute.setNullable(ctx.NOT() == null);
                }
                if (ctx.PRIMARY() != null) {
                    attribute.setPrimaryKey(true);
                }
                if (ctx.UNIQUE() != null) {
                    attribute.setUniqueKey(true);
                }
                attributes.setConstraints(Collections.singletonList(attribute));
            } else if (ctx.CHECK() != null) {
                Expression expr = new OracleExpressionFactory(ctx.expr()).generate();
                attributes.setConstraints(Collections.singletonList(
                        new InLineCheckConstraint(ctx, name, state, expr)));
            } else {
                attributes.setConstraints(Collections.singletonList(new InLineForeignConstraint(
                        ctx, name, state, visitForeignReference(ctx.references_clause()))));
            }
        }
        return attributes;
    }

    public static Boolean isUsingBTree(Index_using_algorithmContext ctx) {
        if (ctx == null) {
            return null;
        } else if (ctx.BTREE() == null) {
            return null;
        }
        return true;
    }

    public static Boolean isUsingHash(Index_using_algorithmContext ctx) {
        if (ctx == null) {
            return null;
        } else if (ctx.HASH() == null) {
            return null;
        }
        return true;
    }

    private ColumnAttributes visitGeneratedColumnAttributeList(Opt_generated_column_attribute_listContext cxt) {
        ColumnAttributes attributes = new ColumnAttributes(cxt);
        if (cxt.opt_generated_column_attribute_list() != null) {
            attributes.merge(visitGeneratedColumnAttributeList(cxt.opt_generated_column_attribute_list()));
        }
        attributes.merge(visitGeneratedColumnAttribute(cxt.generated_column_attribute()));
        return attributes;
    }

    private ColumnAttributes visitGeneratedColumnAttribute(Generated_column_attributeContext ctx) {
        ColumnAttributes attributes = new ColumnAttributes(ctx);
        if (ctx.ID() != null) {
            attributes.setId(Integer.valueOf(ctx.INTNUM().getText()));
        } else if (ctx.COMMENT() != null) {
            attributes.setComment(ctx.STRING_VALUE().getText());
        } else {
            String name = null;
            if (ctx.constraint_and_name() != null) {
                name = ctx.constraint_and_name().constraint_name().getText();
            }
            ConstraintState state = visitConstraintState(ctx.constraint_state());
            InLineConstraint attribute = new InLineConstraint(ctx, name, state);
            if (ctx.CHECK() != null) {
                attribute = new InLineCheckConstraint(ctx, name, state,
                        new OracleExpressionFactory(ctx.expr()).generate());
            }
            if (ctx.NULLX() != null) {
                attribute.setNullable(ctx.NOT() == null);
            }
            if (ctx.PRIMARY() != null) {
                attribute.setPrimaryKey(true);
            }
            if (ctx.UNIQUE() != null) {
                attribute.setUniqueKey(true);
            }
            if (ctx.PRIMARY() == null && ctx.UNIQUE() == null && ctx.KEY() != null) {
                attribute.setPrimaryKey(true);
            }
            attributes.setConstraints(Collections.singletonList(attribute));
        }
        return attributes;
    }

    private Expression visitNowOrSignedLiteral(Now_or_signed_literalContext context) {
        if (context.cur_timestamp_func_params() != null) {
            return visitCurTimestampFuncParams(context.cur_timestamp_func_params());
        }
        return visitSignedLiteralParams(context.signed_literal_params());
    }

    private ConstraintState visitConstraintState(Constraint_stateContext context) {
        if (context == null || context.getChildCount() == 0) {
            return null;
        }
        ConstraintState constraintState = new ConstraintState(context);
        if (context.RELY() != null || context.NORELY() != null) {
            constraintState.setRely(context.RELY() != null);
        }
        if (context.USING() != null && context.INDEX() != null) {
            constraintState.setUsingIndexFlag(true);
            IndexOptions indexOptions = getIndexOptions(null, context.opt_index_options());
            constraintState.setIndexOptions(indexOptions);
        }
        if (context.enable_option() != null) {
            constraintState.setEnable(context.enable_option().ENABLE() != null);
        }
        if (context.VALIDATE() != null || context.NOVALIDATE() != null) {
            constraintState.setValidate(context.VALIDATE() != null);
        }
        return constraintState;
    }

    private IndexOptions getIndexOptions(Index_using_algorithmContext c1,
            Opt_index_optionsContext c2) {
        if (c1 == null && c2 == null) {
            return null;
        } else if (c2 != null && c1 == null) {
            return new OracleIndexOptionsFactory(c2).generate();
        } else if (c2 == null) {
            IndexOptions indexOptions = new IndexOptions(c1);
            indexOptions.setUsingBtree(isUsingBTree(c1));
            indexOptions.setUsingHash(isUsingHash(c1));
            return indexOptions;
        }
        IndexOptions first = getIndexOptions(c1, null);
        IndexOptions second = getIndexOptions(null, c2);
        if (second.getUsingHash() == null) {
            second.setUsingHash(first.getUsingHash());
        }
        if (second.getUsingBtree() == null) {
            second.setUsingBtree(first.getUsingBtree());
        }
        return second;
    }

    private ForeignReference visitForeignReference(References_clauseContext context) {
        Normal_relation_factorContext c = context.normal_relation_factor();
        String schema = null;
        if (c.database_factor() != null) {
            schema = c.database_factor().getText();
        }
        String tableName = c.relation_name().getText();
        List<ColumnReference> columns = new ArrayList<>();
        if (context.column_name_list() != null) {
            columns = context.column_name_list().column_name().stream()
                    .map(c1 -> new ColumnReference(c1, null, null, c1.getText()))
                    .collect(Collectors.toList());
        }
        ForeignReference foreignReference = new ForeignReference(context, schema, tableName, columns);
        if (c.USER_VARIABLE() != null) {
            foreignReference.setUserVariable(c.USER_VARIABLE().getText());
        }
        if (context.reference_option() != null) {
            Reference_optionContext i = context.reference_option();
            if (i.reference_action().CASCADE() != null) {
                foreignReference.setDeleteOption(OnOption.CASCADE);
            } else if (i.reference_action().SET() != null) {
                foreignReference.setDeleteOption(OnOption.SET_NULL);
            }
        }
        return foreignReference;
    }

    private Expression visitCurTimestampFuncParams(Cur_timestamp_func_paramsContext context) {
        if (context.cur_timestamp_func_params() != null) {
            return visitCurTimestampFuncParams(context.cur_timestamp_func_params());
        }
        Cur_timestamp_funcContext ctx = context.cur_timestamp_func();
        String funcName = ctx.getChild(0).getText();
        List<FunctionParam> params = new ArrayList<>();
        if (ctx.INTNUM() != null) {
            params.add(new ExpressionParam(new ConstExpression(ctx.INTNUM())));
        }
        return new FunctionCall(context, funcName, params);
    }

    private Expression visitSignedLiteralParams(Signed_literal_paramsContext context) {
        if (context.signed_literal_params() != null) {
            return visitSignedLiteralParams(context.signed_literal_params());
        }
        return OracleExpressionFactory.getExpression(context.signed_literal());
    }

    private void getSkipIndexTypes(Opt_skip_index_type_listContext ctx, List<String> types) {
        if (ctx.opt_skip_index_type_list() != null) {
            getSkipIndexTypes(ctx.opt_skip_index_type_list(), types);
        }
        if (ctx.skip_index_type() != null) {
            types.add(ctx.skip_index_type().getText());
        }
    }

    private interface ColumnContextProvider {
        ParserRuleContext getSelf();

        Data_typeContext getDataTypeCtx();

        Column_definition_refContext getColumnDefinitionRefCtx();

        Visibility_optionContext getVisibilityOptionCtx();

        TerminalNode getAsNode();

        Opt_identity_attributeContext getOptIdentityAttributeCtx();

        Sequence_option_listContext getSequenceOptionListCtx();

        Bit_exprContext getBitExprCtx();

        Opt_generated_option_listContext getOptGeneratedOptionListCtx();

        TerminalNode getVirtualNode();

        Opt_column_attribute_listContext getOptColumnAttributeListCtx();

        Opt_generated_column_attribute_listContext getOptGeneratedColumnAttributeListCtx();
    }

}
