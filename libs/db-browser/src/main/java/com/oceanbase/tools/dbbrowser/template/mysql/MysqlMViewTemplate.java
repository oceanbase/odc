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
package com.oceanbase.tools.dbbrowser.template.mysql;

import static com.oceanbase.tools.dbbrowser.model.DBConstraintType.PRIMARY_KEY;

import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewSyncSchedule;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 16:45
 * @since: 4.3.4
 */
public class MysqlMViewTemplate implements DBObjectTemplate<DBMaterializedView> {
    private MySQLViewTemplate mySQLViewTemplate;

    private DBTableConstraintEditor dbTableConstraintEditor;

    private DBTablePartitionEditor dbTablePartitionEditor;


    public MysqlMViewTemplate() {
        mySQLViewTemplate = new MySQLViewTemplate();
        dbTableConstraintEditor = new MySQLConstraintEditor();
        dbTablePartitionEditor = new OBMySQLDBTablePartitionEditor();
    }

    @Override
    public String generateCreateObjectTemplate(DBMaterializedView dbObject) {
        Validate.notBlank(dbObject.getName(), "Materialized view name can not be blank");
        DBView dbView = dbObject.generateDBView();
        mySQLViewTemplate.validOperations(dbView);
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("create materialized view ")
                .append(getFullyQualifiedTableName(dbObject));
        // build sql about primary key
        if (CollectionUtils.isNotEmpty(dbObject.getConstraints())) {
            Validate.isTrue(
                    dbObject.getConstraints().size() == 1 && dbObject.getConstraints().get(0).getType() == PRIMARY_KEY,
                    "Only primary key is supported");
            DBTableConstraint dbTableConstraint = dbObject.getConstraints().get(0);
            sqlBuilder.append("(")
                    .append(getPrimary(dbTableConstraintEditor.generateCreateDefinitionDDL(dbTableConstraint)))
                    .append(")");
        }
        // build sql about parallelism degree
        if (Objects.nonNull(dbObject.getParallelismDegree()) && dbObject.getParallelismDegree() > 1) {
            sqlBuilder.line().append("PARALLEL ").append(dbObject.getParallelismDegree());
        }
        // build sql about partition
        if (Objects.nonNull(dbObject.getPartition())) {
            sqlBuilder.line()
                    .append(dbTablePartitionEditor.generateCreateDefinitionDDL(dbObject.getPartition()));
        }
        // build sql about column group
        if (CollectionUtils.isNotEmpty(dbObject.getColumnGroups())) {
            sqlBuilder.line().append(" WITH COLUMN GROUP(")
                    .append(dbObject.getColumnGroups().stream().map(DBColumnGroupElement::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        // build sql about sync data method
        if (Objects.nonNull(dbObject.getSyncDataMethod())) {
            sqlBuilder.line().append(dbObject.getSyncDataMethod().getCreateName());
        }
        // build sql about sync schedule
        if (Objects.nonNull(dbObject.getSyncSchedule())) {
            DBMaterializedViewSyncSchedule syncSchedule = dbObject.getSyncSchedule();
            if (syncSchedule.getStartStrategy() == DBMaterializedViewSyncSchedule.StartStrategy.START_NOW) {
                sqlBuilder.line().append("START WITH sysdate()");
                sqlBuilder.line().append("NEXT sysdate() + INTERVAL ").append(syncSchedule.getInterval()).append(" ")
                        .append(syncSchedule.getUnit());
            } else if (syncSchedule.getStartStrategy() == DBMaterializedViewSyncSchedule.StartStrategy.START_AT) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = formatter.format(syncSchedule.getStartWith());
                sqlBuilder.line().append("START WITH TIMESTAMP '").append(formattedDate).append("'");
                sqlBuilder.line().append("NEXT TIMESTAMP '").append(formattedDate).append("' + INTERVAL ")
                        .append(syncSchedule.getInterval()).append(" ").append(syncSchedule.getUnit());
            }
        }
        // build sql about query rewrite
        if (Objects.nonNull(dbObject.getEnableQueryRewrite())) {
            if (dbObject.getEnableQueryRewrite()) {
                sqlBuilder.line().append("ENABLE QUERY REWRITE");
            } else {
                sqlBuilder.line().append("DISABLE QUERY REWRITE");
            }
        }
        // build sql about query computation
        if (Objects.nonNull(dbObject.getEnableQueryComputation())) {
            if (dbObject.getEnableQueryComputation()) {
                sqlBuilder.line().append("ENABLE ON QUERY COMPUTATION");
            } else {
                sqlBuilder.line().append("DISABLE ON QUERY COMPUTATION");
            }
        }
        sqlBuilder.line().append("AS");
        // build sql about query statement
        mySQLViewTemplate.generateQueryStatement(dbView, sqlBuilder);
        return sqlBuilder.toString();
    }

    private String getPrimary(@NotNull String input) {
        return input.replaceFirst("(?i)CONSTRAINT\\s*", "");
    }

    private String getFullyQualifiedTableName(@NotNull DBMaterializedView DBMaterializedView) {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();;
        if (StringUtils.isNotEmpty(DBMaterializedView.getSchemaName())) {
            sqlBuilder.identifier(DBMaterializedView.getSchemaName()).append(".");
        }
        sqlBuilder.identifier(DBMaterializedView.getName());
        return sqlBuilder.toString();
    }
}
