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

package com.oceanbase.odc.service.structurecompare.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskResultEntity;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;

import lombok.Data;
import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/12/8
 * @since ODC_release_4.2.4
 */
@Data
public class DBObjectComparisonResult {
    private DBObjectType dbObjectType;
    private String dbObjectName;
    private String sourceSchemaName;
    private String targetSchemaName;
    private String sourceDdl;
    private String targetDdl;
    private ComparisonResult comparisonResult;
    /**
     * For TABLE object, such as COLUMN, INDEX, CONSTRAINT and PARTITION when comparisonResult is
     * UPDATE.
     */
    private List<DBObjectComparisonResult> subDBObjectComparisonResult = new ArrayList<>();
    private String changeScript;

    public DBObjectComparisonResult(DBObjectType dbObjectType, String dbObjectName, String sourceSchemaName,
            String targetSchemaName) {
        this.dbObjectType = dbObjectType;
        this.dbObjectName = dbObjectName;
        this.sourceSchemaName = sourceSchemaName;
        this.targetSchemaName = targetSchemaName;
    }

    public DBObjectComparisonResult(DBObjectType dbObjectType, String sourceSchemaName, String targetSchemaName) {
        this.dbObjectType = dbObjectType;
        this.sourceSchemaName = sourceSchemaName;
        this.targetSchemaName = targetSchemaName;
    }

    public StructureComparisonTaskResultEntity toEntity(@NonNull Long structureComparisonTaskId,
            @NonNull DialectType dialectType) {
        StructureComparisonTaskResultEntity entity = new StructureComparisonTaskResultEntity();
        entity.setComparisonTaskId(structureComparisonTaskId);
        entity.setDatabaseObjectType(dbObjectType);
        entity.setDatabaseObjectName(dbObjectName);
        entity.setComparingResult(comparisonResult);
        entity.setSourceObjectDdl(sourceDdl);
        entity.setTargetObjectDdl(targetDdl);

        /**
         * DDL operations involving deletion of database objects are placed in comments
         */
        if (ComparisonResult.ONLY_IN_TARGET == comparisonResult) {
            entity.setChangeSqlScript("/*\n " + changeScript + "*/\n");
            return entity;
        }
        StringBuilder totalSubScript = new StringBuilder();
        if (!subDBObjectComparisonResult.isEmpty()) {
            for (DBObjectComparisonResult subResult : subDBObjectComparisonResult) {
                if (subResult.getChangeScript() == null || subResult.getChangeScript().isEmpty()) {
                    continue;
                }
                DBObjectType objectType = subResult.getDbObjectType();
                if (subResult.getComparisonResult() == ComparisonResult.ONLY_IN_TARGET) {
                    totalSubScript.append("/*\n")
                            .append(subResult.getChangeScript())
                            .append("*/\n\n");
                } else if (objectType == DBObjectType.PARTITION || objectType == DBObjectType.CONSTRAINT
                        || objectType == DBObjectType.INDEX) {
                    List<String> sqls = SqlUtils.split(dialectType, subResult.getChangeScript(), ";");
                    for (String sql : sqls) {
                        String sqlWithoutComment =
                                SqlUtils.removeComments(new SqlCommentProcessor(dialectType, false, false), sql);
                        String comments = sql.replace(sqlWithoutComment, "");
                        if (SqlType.DROP.equals(parseSingleSqlType(dialectType, sqlWithoutComment))) {
                            totalSubScript.append(comments)
                                    .append("/*\n")
                                    .append(appendDelimiterIfNotExists(sqlWithoutComment))
                                    .append("*/\n\n");
                        } else {
                            if (StringUtils.isNotEmpty(sqlWithoutComment)) {
                                totalSubScript.append(appendDelimiterIfNotExists(sqlWithoutComment));
                            }
                            if (StringUtils.isNotEmpty(comments)) {
                                totalSubScript.append(comments).append("\n");
                            }
                        }
                    }
                } else {
                    totalSubScript.append(subResult.getChangeScript()).append("\n");
                }
            }
        }
        if (StringUtils.isNotEmpty(changeScript)) {
            entity.setChangeSqlScript(totalSubScript + "\n" + changeScript);
        } else {
            entity.setChangeSqlScript(totalSubScript.toString());
        }
        return entity;
    }

    private SqlType parseSingleSqlType(DialectType dialectType, String sql) {
        if (Objects.isNull(sql) || sql.isEmpty()) {
            return null;
        }
        try {
            AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0);
            Validate.notNull(factory, "AbstractSyntaxTreeFactory can not be null");
            SqlType sqlType = factory.buildAst(sql).getParseResult().getSqlType();
            Statement stmt = factory.buildAst(sql).getStatement();
            if (isDropPartitionStatement(stmt) || isDropConstraintStatement(stmt) || isDropIndexStatement(stmt)) {
                sqlType = SqlType.DROP;
            }
            return sqlType;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isDropPartitionStatement(Statement stmt) {
        if (stmt instanceof AlterTable) {
            return ((AlterTable) stmt).getAlterTableActions().stream().filter(Objects::nonNull)
                    .anyMatch(action -> !getSafeList(action.getDropPartitionNames()).isEmpty()
                            || !getSafeList(action.getDropSubPartitionNames()).isEmpty());
        }
        return false;
    }

    private boolean isDropConstraintStatement(Statement stmt) {
        if (stmt instanceof AlterTable) {
            return ((AlterTable) stmt).getAlterTableActions().stream().filter(Objects::nonNull)
                    .anyMatch(action -> action.getDropForeignKeyName() != null
                            || !getSafeList(action.getDropConstraintNames()).isEmpty());
        }
        return false;
    }

    private boolean isDropIndexStatement(Statement stmt) {
        if (stmt instanceof AlterTable) {
            return ((AlterTable) stmt).getAlterTableActions().stream().filter(Objects::nonNull)
                    .anyMatch(action -> action.getDropIndexName() != null || action.getDropPrimaryKey());
        }
        return false;
    }

    private List<String> getSafeList(List<String> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private String appendDelimiterIfNotExists(String sql) {
        String s = sql.trim();
        if (StringUtils.isBlank(s) || s.endsWith(";")) {
            return sql + "\n";
        }
        return sql + ";\n";
    }
}
