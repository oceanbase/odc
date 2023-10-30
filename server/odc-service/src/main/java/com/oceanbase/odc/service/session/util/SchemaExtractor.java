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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLFromReferenceFactory;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleFromReferenceFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_database_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Database_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Dot_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Normal_relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factor_with_starContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2023/8/10 13:59
 * @Description: []
 */
public class SchemaExtractor {

    public static Set<String> listSchemaNames(List<String> sqls, DialectType dialectType) {
        Set<String> databaseNames = new HashSet<>();
        for (String sql : sqls) {
            if (dialectType.isMysql()) {
                OBMySQLParser sqlParser = new OBMySQLParser();
                try {
                    ParseTree root = sqlParser.buildAst(new StringReader(sql));
                    OBMySQLRelationFactorVisitor visitor =
                            new OBMySQLRelationFactorVisitor();
                    visitor.visit(root);
                    List<RelationFactor> relationFactorList = visitor.getRelationFactorList();
                    databaseNames
                            .addAll(relationFactorList.stream().map(
                                    relationFactor -> StringUtils.unquoteMySqlIdentifier(relationFactor.getSchema()))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet()));
                } catch (Exception ex) {
                    // just eat exception due to parse failed
                }

            } else if (dialectType.isOracle()) {
                OBOracleSQLParser sqlParser = new OBOracleSQLParser();
                try {
                    ParseTree root = sqlParser.buildAst(new StringReader(sql));
                    OBOracleRelationFactorVisitor visitor =
                            new OBOracleRelationFactorVisitor();
                    visitor.visit(root);
                    List<RelationFactor> relationFactorList = visitor.getRelationFactorList();
                    databaseNames
                            .addAll(relationFactorList.stream().map(relationFactor -> {
                                String schema = relationFactor.getSchema();
                                if (StringUtils.startsWith(schema, "\"") && StringUtils.endsWith(schema, "\"")) {
                                    schema = StringUtils.unquoteOracleIdentifier(schema);
                                } else {
                                    schema = StringUtils.upperCase(schema);
                                }
                                return schema;
                            }).filter(Objects::nonNull)
                                    .collect(Collectors.toSet()));
                } catch (Exception ex) {
                    // just eat exception due to parse failed
                }
            }
        }
        return databaseNames;
    }

    private static class OBMySQLRelationFactorVisitor extends OBParserBaseVisitor<RelationFactor> {
        @Getter
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

    private static class OBOracleRelationFactorVisitor extends
            com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor<RelationFactor> {
        @Getter
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

}
