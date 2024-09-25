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
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTree;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.dbbrowser.parser.result.ParseMysqlPLResult;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLExpressionFactory;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLFromReferenceFactory;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleExpressionFactory;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleFromReferenceFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Database_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Dot_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Normal_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factor_with_starContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Simple_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Use_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.IdentContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Sp_call_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Sp_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParserBaseVisitor;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Current_schemaContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Routine_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Var_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.IdentifierContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Pl_schema_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Sql_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author gaoda.xy
 * @date 2024/5/7 11:53
 */
public class DBSchemaExtractor {

    public static Optional<String> extractSwitchedSchemaName(List<SqlTuple> sqlTuples, DialectType dialectType) {
        Optional<String> schemaName = Optional.empty();
        for (SqlTuple sqlTuple : sqlTuples) {
            try {
                AbstractSyntaxTree ast = sqlTuple.getAst();
                if (ast == null) {
                    sqlTuple.initAst(AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0));
                    ast = sqlTuple.getAst();
                }
                if (dialectType.isMysql() || dialectType.isDoris()) {
                    OBMySQLUseDatabaseStmtVisitor visitor = new OBMySQLUseDatabaseStmtVisitor();
                    visitor.visit(ast.getRoot());
                    if (!visitor.getSchemaSet().isEmpty()) {
                        schemaName = Optional.of(visitor.getSchemaSet().iterator().next());
                    }
                } else if (dialectType.isOracle()) {
                    OBOracleCurrentSchemaVisitor visitor = new OBOracleCurrentSchemaVisitor();
                    visitor.visit(ast.getRoot());
                    if (!visitor.getSchemaSet().isEmpty()) {
                        schemaName = Optional.of(visitor.getSchemaSet().iterator().next());
                    }
                }
            } catch (Exception e) {
                // just eat exception due to parse failed
            }
        }
        return schemaName;
    }

    /**
     * 列出给定SQL元组列表中每个元组所属的数据库模式及其对应的SQL类型
     *
     * @param sqlTuples SQL元组列表
     * @param dialectType 方言类型
     * @param defaultSchema 默认模式
     * @return Map<DBSchemaIdentity, Set < SqlType>> 数据库模式与对应的SQL类型的映射关系
     */
    public static Map<DBSchemaIdentity, Set<SqlType>> listDBSchemasWithSqlTypes(List<SqlTuple> sqlTuples,
            DialectType dialectType, String defaultSchema) {
        Map<DBSchemaIdentity, Set<SqlType>> res = new HashMap<>();
        for (SqlTuple sqlTuple : sqlTuples) {
            try {
                AbstractSyntaxTree ast = sqlTuple.getAst();
                if (ast == null) {
                    sqlTuple.initAst(AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0));
                    ast = sqlTuple.getAst();
                    // 解析失败时忽略异常
                }
                // 获取SQL元组所属的数据库模式
                Set<DBSchemaIdentity> identities = listDBSchemas(ast, dialectType, defaultSchema);
                SqlType sqlType = SqlType.OTHERS;
                BasicResult basicResult = ast.getParseResult();
                // 获取SQL类型
                if (Objects.nonNull(basicResult) && Objects.nonNull(basicResult.getSqlType())
                        && basicResult.getSqlType() != SqlType.UNKNOWN) {
                    sqlType = basicResult.getSqlType();
                    // 解析失败时忽略异常
                }
                // 将数据库模式与对应的SQL类型添加到映射关系中
                for (DBSchemaIdentity identity : identities) {
                    res.computeIfAbsent(identity, k -> new HashSet<>()).add(sqlType);
                    // 解析失败时忽略异常
                }
            } catch (Exception e) {
                // just eat exception due to parse failed
                // 解析失败时忽略异常
            }
            // 解析失败时忽略异常
        }
        return res;
        // 解析失败时忽略异常
    }

    private static Set<DBSchemaIdentity> listDBSchemas(AbstractSyntaxTree ast, DialectType dialectType,
            String defaultSchema) {
        Set<DBSchemaIdentity> identities = new HashSet<>();
        BasicResult basicResult = ast.getParseResult();
        // 判断方言类型是否为MySQL或Doris
        if (dialectType.isMysql() || dialectType.isDoris()) {
            if (basicResult.isPlDdl() || basicResult instanceof ParseMysqlPLResult) {
                OBMySQLPLRelationFactorVisitor visitor = new OBMySQLPLRelationFactorVisitor();
                // 访问AST根节点
                visitor.visit(ast.getRoot());
                // 获取数据库模式标识符集合
                identities = visitor.getIdentities();
            } else {
                OBMySQLRelationFactorVisitor visitor = new OBMySQLRelationFactorVisitor();
                // 访问AST根节点
                visitor.visit(ast.getRoot());
                // 获取数据库模式标识符集合
                identities = visitor.getIdentities();
            }
            identities = identities.stream()
                    .filter(e -> Objects.nonNull(e.getSchema()) || Objects.nonNull(e.getTable())).map(e -> {
                        DBSchemaIdentity i = new DBSchemaIdentity(e.getSchema(), e.getTable());
                        // 获取模式，如果为空则使用默认模式
                        String schema = StringUtils.isNotBlank(i.getSchema()) ? i.getSchema() : defaultSchema;
                        // 去除模式中的转义字符
                        i.setSchema(StringUtils.unquoteMySqlIdentifier(schema));
                        // 去除表中的转义字符
                        i.setTable(StringUtils.unquoteMySqlIdentifier(i.getTable()));
                        return i;
                    }).collect(Collectors.toSet());
            // 判断方言类型是否为Oracle
        } else if (dialectType.isOracle()) {
            if (basicResult.isPlDdl() || basicResult instanceof ParseOraclePLResult) {
                OBOraclePLRelationFactorVisitor visitor = new OBOraclePLRelationFactorVisitor();
                // 访问AST根节点
                visitor.visit(ast.getRoot());
                // 获取数据库模式标识符集合
                identities = visitor.getIdentities();
            } else {
                OBOracleRelationFactorVisitor visitor = new OBOracleRelationFactorVisitor();
                // 访问AST根节点
                visitor.visit(ast.getRoot());
                // 获取数据库模式标识符集合
                identities = visitor.getIdentities();
            }
            identities = identities.stream()
                    .filter(e -> Objects.nonNull(e.getSchema()) || Objects.nonNull(e.getTable())).map(e -> {
                        DBSchemaIdentity i = new DBSchemaIdentity(e.getSchema(), e.getTable());
                        // 获取模式，如果为空则使用默认模式
                        String schema = StringUtils.isNotBlank(i.getSchema()) ? i.getSchema() : defaultSchema;
                        // 判断模式是否被引号包裹
                        if (StringUtils.startsWith(schema, "\"") && StringUtils.endsWith(schema, "\"")) {
                            schema = StringUtils.unquoteOracleIdentifier(schema);
                        } else {
                            schema = StringUtils.upperCase(schema);
                        }
                        String table = StringUtils.isNotBlank(i.getTable()) ? i.getTable() : null;
                        // 判断表是否被引号包裹
                        if (StringUtils.startsWith(table, "\"") && StringUtils.endsWith(table, "\"")) {
                            table = StringUtils.unquoteOracleIdentifier(table);
                        } else {
                            table = StringUtils.upperCase(table);
                        }
                        i.setSchema(schema);
                        i.setTable(table);
                        return i;
                    }).collect(Collectors.toSet());
        }
        return identities;
    }


    @Getter
    private static class OBMySQLRelationFactorVisitor extends OBParserBaseVisitor<RelationFactor> {

        private final Set<DBSchemaIdentity> identities = new HashSet<>();

        @Override
        public RelationFactor visitRelation_factor(Relation_factorContext ctx) {
            addRelationFactor(MySQLFromReferenceFactory.getRelationFactor(ctx));
            return null;
        }

        @Override
        public RelationFactor visitNormal_relation_factor(Normal_relation_factorContext ctx) {
            addRelationFactor(MySQLFromReferenceFactory.getRelationFactor(ctx));
            return null;
        }

        @Override
        public RelationFactor visitDot_relation_factor(Dot_relation_factorContext ctx) {
            addRelationFactor(new RelationFactor(ctx, ctx.relation_name().getText()));
            return null;
        }

        @Override
        public RelationFactor visitRelation_factor_with_star(Relation_factor_with_starContext ctx) {
            ctx.relation_name().forEach(r -> {
                addRelationFactor(new RelationFactor(ctx, r.getText()));
            });
            return null;
        }

        @Override
        public RelationFactor visitDatabase_factor(Database_factorContext ctx) {
            RelationFactor relationFactor = new RelationFactor(ctx, "");
            relationFactor.setSchema(ctx.relation_name().getText());
            identities.add(new DBSchemaIdentity(relationFactor.getSchema(), null));
            return null;
        }

        @Override
        public RelationFactor visitCreate_database_stmt(Create_database_stmtContext ctx) {
            return null;
        }

        @Override
        public RelationFactor visitSimple_expr(Simple_exprContext ctx) {
            MySQLExpressionFactory expressionFactory = new MySQLExpressionFactory();
            Expression expr = expressionFactory.visit(ctx);
            if (expr instanceof FunctionCall) {
                String relationName = ((FunctionCall) expr).getFunctionName();
                RelationFactor relationFactor = new RelationFactor(relationName);
                if (relationName.contains(".")) {
                    String[] names = relationName.split("\\.");
                    relationFactor.setSchema(names[0]);
                }
                identities.add(new DBSchemaIdentity(relationFactor.getSchema(), null));
            }
            return null;
        }

        private void addRelationFactor(RelationFactor rf) {
            if (StringUtils.isBlank(rf.getUserVariable())) {
                identities.add(new DBSchemaIdentity(rf.getSchema(), rf.getRelation()));
            }
        }

    }


    @Getter
    private static class OBMySQLPLRelationFactorVisitor extends PLParserBaseVisitor<RelationFactor> {

        private final Set<DBSchemaIdentity> identities = new HashSet<>();
        private final List<RelationFactor> relationFactorList = new ArrayList<>();

        @Override
        public RelationFactor visitSp_name(Sp_nameContext ctx) {
            List<IdentContext> idents = ctx.ident();
            RelationFactor rf;
            if (idents.size() == 1) {
                rf = new RelationFactor(idents.get(0).getText());
            } else {
                rf = new RelationFactor(idents.get(idents.size() - 1).getText());
                rf.setSchema(idents.get(0).getText());
            }
            identities.add(new DBSchemaIdentity(rf.getSchema(), null));
            return null;
        }

        @Override
        public RelationFactor visitSp_call_name(Sp_call_nameContext ctx) {
            List<IdentContext> idents = ctx.ident();
            RelationFactor rf;
            if (idents.size() == 1) {
                rf = new RelationFactor(idents.get(0).getText());
            } else {
                // If there exists two idents, we can not determine weather it is a schema or a package (because OB
                // MySQL holds system package such as `dbms_stats`) name. The provisional program is ignoring the
                // package name and always treat it as schema name.
                rf = new RelationFactor(idents.get(idents.size() - 1).getText());
                rf.setSchema(idents.get(0).getText());
            }
            identities.add(new DBSchemaIdentity(rf.getSchema(), null));
            return null;
        }

    }


    @Getter
    private static class OBMySQLUseDatabaseStmtVisitor extends OBParserBaseVisitor<Void> {

        private final Set<String> schemaSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        @Override
        public Void visitUse_database_stmt(Use_database_stmtContext ctx) {
            Relation_nameContext relationName = ctx.database_factor().relation_name();
            schemaSet.add(StringUtils.unquoteMySqlIdentifier(relationName.getText()));
            return null;
        }

    }


    @Getter
    private static class OBOracleRelationFactorVisitor
            extends com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor<RelationFactor> {

        private final Set<DBSchemaIdentity> identities = new HashSet<>();

        @Override
        public RelationFactor visitRelation_factor(OBParser.Relation_factorContext ctx) {
            addRelationFactor(OracleFromReferenceFactory.getRelationFactor(ctx));
            return null;
        }

        @Override
        public RelationFactor visitNormal_relation_factor(OBParser.Normal_relation_factorContext ctx) {
            addRelationFactor(OracleFromReferenceFactory.getRelationFactor(ctx));
            return null;
        }

        @Override
        public RelationFactor visitDot_relation_factor(OBParser.Dot_relation_factorContext ctx) {
            addRelationFactor(new RelationFactor(ctx, ctx.relation_name().getText()));
            return null;
        }

        @Override
        public RelationFactor visitDatabase_factor(OBParser.Database_factorContext ctx) {
            RelationFactor relationFactor = new RelationFactor(ctx, "");
            relationFactor.setSchema(ctx.relation_name().getText());
            identities.add(new DBSchemaIdentity(relationFactor.getSchema(), null));
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
            identities.add(new DBSchemaIdentity(relationFactor.getSchema(), null));
            return null;
        }

        @Override
        public RelationFactor visitCurrent_schema(OBParser.Current_schemaContext ctx) {
            OBParser.Relation_nameContext relationName = ctx.relation_name();
            RelationFactor relationFactor = new RelationFactor(ctx, "");
            relationFactor.setSchema(relationName.getText());
            identities.add(new DBSchemaIdentity(relationFactor.getSchema(), null));
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
                    identities.add(new DBSchemaIdentity(relationFactor.getSchema(), null));
                }
            } else if (expr instanceof FunctionCall) {
                RelationFactor relationFactor = new RelationFactor(((FunctionCall) expr).getFunctionName());
                identities.add(new DBSchemaIdentity(relationFactor.getSchema(), null));
            }
            return null;
        }

        private void addRelationFactor(RelationFactor rf) {
            if (StringUtils.isBlank(rf.getUserVariable())) {
                identities.add(new DBSchemaIdentity(rf.getSchema(), rf.getRelation()));
            }
        }

    }


    @Getter
    private static class OBOraclePLRelationFactorVisitor
            extends com.oceanbase.tools.sqlparser.oboracle.PLParserBaseVisitor<RelationFactor> {

        private final List<RelationFactor> relationFactorList = new ArrayList<>();
        private final Set<DBSchemaIdentity> identities = new HashSet<>();

        @Override
        public RelationFactor visitPl_schema_name(Pl_schema_nameContext ctx) {
            List<IdentifierContext> identifiers = ctx.identifier();
            RelationFactor rf;
            if (identifiers.size() == 1) {
                rf = new RelationFactor(identifiers.get(0).getText());
            } else {
                // If there exists two identifiers, we can not determine weather it is a schema or a package name.
                // The provisional program is ignoring the package name and always treat it as schema name.
                rf = new RelationFactor(identifiers.get(identifiers.size() - 1).getText());
                rf.setSchema(identifiers.get(0).getText());
            }
            identities.add(new DBSchemaIdentity(rf.getSchema(), null));
            return null;
        }

        @Override
        public RelationFactor visitSql_stmt(Sql_stmtContext ctx) {
            try {
                OBOracleRelationFactorVisitor visitor = new OBOracleRelationFactorVisitor();
                visitor.visit(ctx);
                identities.addAll(visitor.getIdentities());
            } catch (Exception e) {
                // eat the exception
            }
            return null;
        }

    }


    @Getter
    private static class OBOracleCurrentSchemaVisitor
            extends com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor<Void> {

        private final Set<String> schemaSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        @Override
        public Void visitCurrent_schema(Current_schemaContext ctx) {
            OBParser.Relation_nameContext relationName = ctx.relation_name();
            schemaSet.add(StringUtils.unquoteOracleIdentifier(relationName.getText()));
            return null;
        }

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class DBSchemaIdentity {

        private String schema;
        private String table;

    }

}
