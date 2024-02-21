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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTree;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLFromReferenceFactory;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleFromReferenceFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Database_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Dot_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Normal_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factor_with_starContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.IdentContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Sp_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParserBaseVisitor;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.IdentifierContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Pl_schema_nameContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2023/8/10 13:59
 * @Description: []
 */
public class SchemaExtractor {

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

    public static Set<String> listSchemaNames(List<SqlTuple> sqlTuples, String defaultSchema, DialectType dialectType) {
        return sqlTuples.stream().flatMap(sqlTuple -> {
            try {
                AbstractSyntaxTree ast = sqlTuple.getAst();
                if (ast == null) {
                    sqlTuple.initAst(AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0));
                    ast = sqlTuple.getAst();
                }
                return listSchemaNames(ast, defaultSchema, dialectType).stream();
            } catch (Exception e) {
                // just eat exception due to parse failed
                return Stream.empty();
            }
        }).collect(Collectors.toSet());
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
            if (basicResult.isPlDdl()) {
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
            if (basicResult.isPlDdl()) {
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
    }


    @Getter
    private static class OBMySQLPLRelationFactorVisitor extends PLParserBaseVisitor<RelationFactor> {

        private final List<RelationFactor> relationFactorList = new ArrayList<>();

        @Override
        public RelationFactor visitSp_name(Sp_nameContext ctx) {
            List<IdentContext> idents = ctx.ident();
            if (idents.size() == 1) {
                relationFactorList.add(new RelationFactor(idents.get(0).getText()));
            } else if (idents.size() == 2) {
                RelationFactor relationFactor = new RelationFactor(idents.get(1).getText());
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

    public static void main(String[] args) {
        String sql = "CREATE OR REPLACE FUNCTION SCHEMA_NAME.INCREMENT_BY_ONE (INPUT_NUMBER IN NUMBER)\n"
                + "RETURN NUMBER IS\n"
                + "BEGIN\n"
                + "  RETURN INPUT_NUMBER + 1;\n"
                + "END INCREMENT_BY_ONE;";
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0);
        AbstractSyntaxTree ast = factory.buildAst(sql);
        Set<String> schemaNames = listSchemaNames(ast, "schema_name2", DialectType.OB_ORACLE);
        System.out.println(schemaNames);
    }

}
