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

package com.oceanbase.odc.service.session.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTree;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.dbbrowser.parser.result.ParseMysqlPLResult;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLFromReferenceFactory;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleExpressionFactory;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleFromReferenceFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Database_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Dot_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Function_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Normal_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factor_with_starContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Simple_func_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.IdentContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Sp_call_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Sp_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParserBaseVisitor;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Routine_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Var_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.IdentifierContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Pl_schema_nameContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;

import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2023/8/10 13:59
 * @Description: []
 */
public class SchemaExtractor {

    public static Optional<String> extractSwitchedSchemaName(List<SqlTuple> sqlTuples, DialectType dialectType) {
        Optional<String> schemaName = Optional.empty();
        for (SqlTuple sqlTuple : sqlTuples) {
            try {
                AbstractSyntaxTree ast = sqlTuple.getAst();
                if (ast == null) {
                    sqlTuple.initAst(AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0));
                    ast = sqlTuple.getAst();
                }
                BasicResult basicResult = ast.getParseResult();
                if (((dialectType.isMysql() || dialectType.isDoris()) && basicResult.getSqlType() == SqlType.USE_DB)
                        || (dialectType.isOracle() && basicResult.getSqlType() == SqlType.ALTER)) {
                    Set<String> schemaNames = listSchemaNames(ast, null, dialectType);
                    if (CollectionUtils.isNotEmpty(schemaNames)) {
                        schemaName = Optional.of(schemaNames.iterator().next());
                    }
                }
            } catch (Exception e) {
                // just eat exception due to parse failed
            }
        }
        return schemaName;
    }

    public static Map<String, Set<SqlType>> listSchemaName2SqlTypes(List<SqlTuple> sqlTuples, String defaultSchema,
            DialectType dialectType) {
        Map<String, Set<SqlType>> schemaName2SqlTypes = new HashMap<>();
        for (SqlTuple sqlTuple : sqlTuples) {
            try {
                AbstractSyntaxTree ast = sqlTuple.getAst();
                if (ast == null) {
                    sqlTuple.initAst(AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0));
                    ast = sqlTuple.getAst();
                }
                Set<String> schemaNames = listSchemaNames(ast, defaultSchema, dialectType);
                SqlType sqlType = SqlType.OTHERS;
                BasicResult basicResult = ast.getParseResult();
                if (Objects.nonNull(basicResult) && Objects.nonNull(basicResult.getSqlType())
                        && basicResult.getSqlType() != SqlType.UNKNOWN) {
                    sqlType = basicResult.getSqlType();
                }
                for (String schemaName : schemaNames) {
                    Set<SqlType> sqlTypes = schemaName2SqlTypes.computeIfAbsent(schemaName, k -> new HashSet<>());
                    sqlTypes.add(sqlType);
                }
            } catch (Exception e) {
                // just eat exception due to parse failed
            }
        }
        return schemaName2SqlTypes;
    }

    public static Set<String> listSchemaNames(List<String> sqls, DialectType dialectType, String defaultSchema) {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0);
        if (factory == null) {
            return new HashSet<>();
        }
        return sqls.stream().flatMap(sql -> {
            try {
                return listSchemaNames(factory.buildAst(sql), defaultSchema, dialectType).stream();
            } catch (Exception e) {
                // just eat exception due to parse failed
                return Stream.empty();
            }
        }).collect(Collectors.toSet());
    }

    private static Set<String> listSchemaNames(AbstractSyntaxTree ast, String defaultSchema, DialectType dialectType) {
        List<RelationFactor> relationFactorList;
        BasicResult basicResult = ast.getParseResult();
        if (dialectType.isMysql() || dialectType.isDoris()) {
            if (basicResult.isPlDdl() || basicResult instanceof ParseMysqlPLResult) {
                OBMySQLPLRelationFactorVisitor visitor = new OBMySQLPLRelationFactorVisitor();
                visitor.visit(ast.getRoot());
                relationFactorList = visitor.getRelationFactorList();
            } else {
                OBMySQLRelationFactorVisitor visitor = new OBMySQLRelationFactorVisitor();
                visitor.visit(ast.getRoot());
                relationFactorList = visitor.getRelationFactorList();
            }
            return relationFactorList.stream()
                    .filter(r -> StringUtils.isBlank(r.getUserVariable()))
                    .map(r -> {
                        String schema = StringUtils.isNotBlank(r.getSchema()) ? r.getSchema() : defaultSchema;
                        return StringUtils.unquoteMySqlIdentifier(schema);
                    })
                    .filter(Objects::nonNull).collect(Collectors.toSet());
        } else if (dialectType.isOracle()) {
            if (basicResult.isPlDdl() || basicResult instanceof ParseOraclePLResult) {
                OBOraclePLRelationFactorVisitor visitor = new OBOraclePLRelationFactorVisitor();
                visitor.visit(ast.getRoot());
                relationFactorList = visitor.getRelationFactorList();
            } else {
                OBOracleRelationFactorVisitor visitor = new OBOracleRelationFactorVisitor();
                visitor.visit(ast.getRoot());
                relationFactorList = visitor.getRelationFactorList();
            }
            return relationFactorList.stream()
                    .filter(r -> StringUtils.isBlank(r.getUserVariable()))
                    .map(r -> {
                        String schema = StringUtils.isNotBlank(r.getSchema()) ? r.getSchema() : defaultSchema;
                        if (StringUtils.startsWith(schema, "\"") && StringUtils.endsWith(schema, "\"")) {
                            return StringUtils.unquoteOracleIdentifier(schema);
                        }
                        return StringUtils.upperCase(schema);
                    }).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    @Getter
    private static class OBMySQLRelationFactorVisitor extends OBParserBaseVisitor<RelationFactor> {

        private final List<RelationFactor> relationFactorList = new ArrayList<>();

        @Override
        public RelationFactor visitRelation_factor(Relation_factorContext ctx) {
            relationFactorList.add(MySQLFromReferenceFactory.getRelationFactor(ctx));
            return null;
        }

        @Override
        public RelationFactor visitNormal_relation_factor(Normal_relation_factorContext ctx) {
            relationFactorList.add(MySQLFromReferenceFactory.getRelationFactor(ctx));
            return null;
        }

        @Override
        public RelationFactor visitDot_relation_factor(Dot_relation_factorContext ctx) {
            relationFactorList.add(new RelationFactor(ctx, ctx.relation_name().getText()));
            return null;
        }

        @Override
        public RelationFactor visitRelation_factor_with_star(Relation_factor_with_starContext ctx) {
            ctx.relation_name().stream().forEach(r -> relationFactorList.add(new RelationFactor(ctx, r.getText())));
            return null;
        }

        @Override
        public RelationFactor visitDatabase_factor(Database_factorContext ctx) {
            RelationFactor relationFactor = new RelationFactor(ctx, "");
            relationFactor.setSchema(ctx.relation_name().getText());
            relationFactorList.add(relationFactor);
            return null;
        }

        @Override
        public RelationFactor visitCreate_database_stmt(Create_database_stmtContext ctx) {
            return null;
        }

        @Override
        public RelationFactor visitSimple_func_expr(Simple_func_exprContext ctx) {
            Function_nameContext functionName = ctx.function_name();
            com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_nameContext relationName = ctx.relation_name();
            if (Objects.nonNull(functionName) && StringUtils.isNotBlank(functionName.getText())) {
                RelationFactor relationFactor = new RelationFactor(functionName.getText());
                if (Objects.nonNull(relationName) && StringUtils.isNotBlank(relationName.getText())) {
                    relationFactor.setSchema(relationName.getText());
                }
                relationFactorList.add(relationFactor);
            }
            return null;
        }

    }

    @Getter
    private static class OBOracleRelationFactorVisitor extends
            com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor<RelationFactor> {

        private final List<RelationFactor> relationFactorList = new ArrayList<>();

        @Override
        public RelationFactor visitRelation_factor(OBParser.Relation_factorContext ctx) {
            relationFactorList.add(OracleFromReferenceFactory.getRelationFactor(ctx));
            return null;
        }

        @Override
        public RelationFactor visitNormal_relation_factor(OBParser.Normal_relation_factorContext ctx) {
            relationFactorList.add(OracleFromReferenceFactory.getRelationFactor(ctx));
            return null;
        }

        @Override
        public RelationFactor visitDot_relation_factor(OBParser.Dot_relation_factorContext ctx) {
            relationFactorList.add(new RelationFactor(ctx, ctx.relation_name().getText()));
            return null;
        }

        @Override
        public RelationFactor visitDatabase_factor(OBParser.Database_factorContext ctx) {
            RelationFactor relationFactor = new RelationFactor(ctx, "");
            relationFactor.setSchema(ctx.relation_name().getText());
            relationFactorList.add(relationFactor);
            return null;
        }

        @Override
        public RelationFactor visitRoutine_access_name(OBParser.Routine_access_nameContext ctx) {
            List<Var_nameContext> varNames = ctx.var_name();
            Routine_nameContext routineName = ctx.routine_name();
            RelationFactor relationFactor = new RelationFactor(routineName.getText());
            if (CollectionUtils.isNotEmpty(varNames)) {
                // If there exists only one var_name, we can not determine weather it is a schema or a package name.
                // The provisional program is ignoring the package name and always treat it as schema name.
                relationFactor.setSchema(varNames.get(0).getText());
            }
            relationFactorList.add(relationFactor);
            return null;
        }

        @Override
        public RelationFactor visitCurrent_schema(OBParser.Current_schemaContext ctx) {
            Relation_nameContext relationName = ctx.relation_name();
            RelationFactor relationFactor = new RelationFactor(ctx, "");
            relationFactor.setSchema(relationName.getText());
            relationFactorList.add(relationFactor);
            return null;
        }

        @Override
        public RelationFactor visitObj_access_ref(OBParser.Obj_access_refContext ctx) {
            OracleExpressionFactory expressionFactory = new OracleExpressionFactory(ctx);
            Expression expr = expressionFactory.generate();
            if (expr instanceof RelationReference) {
                Expression e = expr;
                while (Objects.nonNull(e.getReference())) {
                    e = e.getReference();
                }
                if (e instanceof FunctionCall) {
                    RelationFactor relationFactor = new RelationFactor(((FunctionCall) e).getFunctionName());
                    relationFactor.setSchema(((RelationReference) expr).getRelationName());
                    relationFactorList.add(relationFactor);
                }
            } else if (expr instanceof FunctionCall) {
                RelationFactor relationFactor = new RelationFactor(((FunctionCall) expr).getFunctionName());
                relationFactorList.add(relationFactor);
            }
            return null;
        }

    }

    @Getter
    private static class OBMySQLPLRelationFactorVisitor extends PLParserBaseVisitor<RelationFactor> {

        private final List<RelationFactor> relationFactorList = new ArrayList<>();

        @Override
        public RelationFactor visitSp_name(Sp_nameContext ctx) {
            List<IdentContext> idents = ctx.ident();
            if (idents.size() == 1) {
                relationFactorList.add(new RelationFactor(idents.get(0).getText()));
            } else {
                RelationFactor relationFactor = new RelationFactor(idents.get(idents.size() - 1).getText());
                relationFactor.setSchema(idents.get(0).getText());
                relationFactorList.add(relationFactor);
            }
            return null;
        }

        @Override
        public RelationFactor visitSp_call_name(Sp_call_nameContext ctx) {
            List<IdentContext> idents = ctx.ident();
            if (idents.size() == 1) {
                relationFactorList.add(new RelationFactor(idents.get(0).getText()));
            } else {
                // If there exists two idents, we can not determine weather it is a schema or a package (because OB
                // MySQL holds system package such as `dbms_stats`) name. The provisional program is ignoring the
                // package name and always treat it as schema name.
                RelationFactor relationFactor = new RelationFactor(idents.get(idents.size() - 1).getText());
                relationFactor.setSchema(idents.get(0).getText());
                relationFactorList.add(relationFactor);
            }
            return null;
        }

    }

    @Getter
    private static class OBOraclePLRelationFactorVisitor
            extends com.oceanbase.tools.sqlparser.oboracle.PLParserBaseVisitor<RelationFactor> {

        private final List<RelationFactor> relationFactorList = new ArrayList<>();

        @Override
        public RelationFactor visitPl_schema_name(Pl_schema_nameContext ctx) {
            List<IdentifierContext> identifiers = ctx.identifier();
            if (identifiers.size() == 1) {
                relationFactorList.add(new RelationFactor(identifiers.get(0).getText()));
            } else if (identifiers.size() == 2) {
                RelationFactor relationFactor = new RelationFactor(identifiers.get(1).getText());
                relationFactor.setSchema(identifiers.get(0).getText());
                relationFactorList.add(relationFactor);
            }
            return null;
        }

    }

}
