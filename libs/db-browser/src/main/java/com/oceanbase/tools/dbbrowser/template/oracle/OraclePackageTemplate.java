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
package com.oceanbase.tools.dbbrowser.template.oracle;

import java.util.List;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLType;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.parser.PLParser;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * {@link OraclePackageTemplate}
 *
 * @author yh263208
 * @date 2022-02-22 16:34
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OraclePackageTemplate extends BaseOraclePLTemplate<DBPackage> {

    /**
     * package_block in PLParser.g4:
     *
     * <pre>
     *   PACKAGE_P pl_schema_name proc_clause_list? is_or_as decl_stmt_list? END_KEY (identifier | sql_keyword_identifier)?
     * </pre>
     */
    private static final Pattern PATTERN = Pattern.compile(
            "((is|as)(\\s|\n|\r|\t)+|^)(type|subtype|cursor|function|procedure)",
            Pattern.CASE_INSENSITIVE);
    private final JdbcOperations jdbcOperations;

    public OraclePackageTemplate(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public String generateCreateObjectTemplate(@NotNull DBPackage dbObject) {
        Validate.notNull(dbObject.getPackageType(), "Package type can not be null");
        Validate.notBlank(dbObject.getPackageName(), "Package name can not be blank");
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("CREATE OR REPLACE ").append(dbObject.type().getName());
        if (DBObjectType.PACKAGE == dbObject.type()) {
            sqlBuilder.append(" ")
                    .identifier(dbObject.getPackageName()).append(" AS")
                    .append("\n\tFUNCTION FUNC_EXAMPLE (p1 IN NUMBER) RETURN NUMBER;")
                    .append("\n\tPROCEDURE PROC_EXAMPLE (p1 IN NUMBER);")
                    .append("\nEND");
            return sqlBuilder.toString();
        }
        String schema = jdbcOperations.query("SELECT SYS_CONTEXT('userenv', 'CURRENT_SCHEMA') FROM DUAL", rs -> {
            if (!rs.next()) {
                throw new IllegalStateException("Empty result set");
            }
            return rs.getString(1);
        });
        SqlBuilder queryBuilder = new OracleSqlBuilder();
        queryBuilder.append("SELECT TEXT FROM ALL_SOURCE WHERE OWNER=").value(schema)
                .append(" AND NAME=").value(dbObject.getPackageName())
                .append(" AND TYPE='PACKAGE'");
        List<String> textList = jdbcOperations.query(queryBuilder.toString(), (rs, rowNum) -> rs.getString(1));
        if (CollectionUtils.isEmpty(textList)) {
            throw new IllegalStateException("Package not found, " + schema + "." + dbObject.getPackageName());
        }
        String ddl = textList.get(0).trim();
        if (!ddl.startsWith("create") && !ddl.startsWith("CREATE")) {
            ddl = "CREATE " + ddl;
        }
        return buildPackageBodyDdl(PLParser.parseOracle(ddl));
    }

    private String buildPackageBodyDdl(ParseOraclePLResult parseResult) {
        String isOrAs = parseResult.getIsOrAs();
        StringBuilder create = new StringBuilder("CREATE OR REPLACE PACKAGE BODY ");
        create.append(parseResult.getPlName())
                .append(" ")
                .append(isOrAs)
                .append("\n");
        for (DBFunction function : parseResult.getFunctionList()) {
            create.append("\n")
                    .append(function.getDdl())
                    .append(" ")
                    .append(isOrAs)
                    .append("\nBEGIN\n\t-- TODO\nEND;\n");
        }
        for (DBProcedure procedure : parseResult.getProcedureList()) {
            create.append("\n")
                    .append(procedure.getDdl())
                    .append(" ")
                    .append(isOrAs)
                    .append("\nBEGIN\n\t-- TODO\nEND;\n");
        }
        for (DBPLType cursor : parseResult.getCursorList()) {
            create.append("\n")
                    .append(cursor.getDdl())
                    .append(" ")
                    .append(isOrAs)
                    .append("\nBEGIN\n\t-- TODO\nEND;\n");
        }
        create.append("\nEND ").append(parseResult.getPlName()).append(";");
        return create.toString();
    }

}
