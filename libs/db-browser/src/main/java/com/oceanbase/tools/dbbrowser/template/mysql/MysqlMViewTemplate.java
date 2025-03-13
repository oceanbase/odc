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

import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBMViewSyncSchedule;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import static com.oceanbase.tools.dbbrowser.model.DBConstraintType.PRIMARY_KEY;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 16:45
 * @since: 4.3.4
 */
public class MysqlMViewTemplate implements DBObjectTemplate<DBMView> {
    private MySQLViewTemplate mySQLViewTemplate;

    private DBTableColumnEditor dbTableColumnEditor;

    private DBTableConstraintEditor dbTableConstraintEditor;

    private DBTablePartitionEditor dbTablePartitionEditor;


    public MysqlMViewTemplate() {
        mySQLViewTemplate = new MySQLViewTemplate();
        dbTableColumnEditor = new MySQLColumnEditor();
        dbTableConstraintEditor = new MySQLConstraintEditor();
        dbTablePartitionEditor = new OBMySQLDBTablePartitionEditor();
    }

    @Override
    public String generateCreateObjectTemplate(DBMView dbObject) {
        Validate.notBlank(dbObject.getName(), "Materialized view name can not be blank");
        DBView dbView = dbObject.generateDBView();
        mySQLViewTemplate.validOperations(dbView);
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("create materialized view ")
                .append(getFullyQualifiedTableName(dbObject));
        boolean isFirstSentence = true;
        // 获取列构造
        if (CollectionUtils.isNotEmpty(dbObject.getConstraints())) {
            Validate.isTrue(dbObject.getConstraints().size() == 1&&dbObject.getConstraints().get(0).getType()==PRIMARY_KEY, "Only primary key is supported");
            DBTableConstraint dbTableConstraint = dbObject.getConstraints().get(0);
            sqlBuilder.append("(").append(getPrimary(dbTableConstraintEditor.generateCreateDefinitionDDL(dbTableConstraint))).append(")");
        }
        // 构造物化视图并行度
        if (Objects.nonNull(dbObject.getParallelismDegree()) && dbObject.getParallelismDegree() > 1) {
            sqlBuilder.line().append("PARALLEL ").append(dbObject.getParallelismDegree());
        }
        // 获取分区构造
        if (Objects.nonNull(dbObject.getPartition())) {
            sqlBuilder.line()
                    .append(dbTablePartitionEditor.generateCreateDefinitionDDL(dbObject.getPartition()));
        }
        // 获取存储格式构造
        if (CollectionUtils.isNotEmpty(dbObject.getColumnGroups())) {
            sqlBuilder.line().append(" WITH COLUMN GROUP(")
                    .append(dbObject.getColumnGroups().stream().map(DBColumnGroupElement::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        // 物化视图刷新方式
        if (Objects.nonNull(dbObject.getSyncDataMethod())) {
            sqlBuilder.line().append(dbObject.getSyncDataMethod().getCreateName());
        }
        if (Objects.nonNull(dbObject.getSyncSchedule())) {
            DBMViewSyncSchedule syncSchedule = dbObject.getSyncSchedule();
            if (syncSchedule.getStartStrategy() == DBMViewSyncSchedule.StartStrategy.START_NOW) {
                sqlBuilder.line().append("START WITH sysdate()");
                sqlBuilder.line().append("NEXT sysdate() + INTERVAL ").append(syncSchedule.getInterval()).append(" ")
                        .append(syncSchedule.getUnit());
            } else if (syncSchedule.getStartStrategy() == DBMViewSyncSchedule.StartStrategy.START_AT) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = formatter.format(syncSchedule.getStartWith());
                sqlBuilder.line().append("START WITH TIMESTAMP '").append(formattedDate).append("'");
                sqlBuilder.line().append("NEXT TIMESTAMP '").append(formattedDate).append("' + INTERVAL ")
                        .append(syncSchedule.getInterval()).append(" ").append(syncSchedule.getUnit());
            }
        }
        // 查询改写
        if (Objects.nonNull(dbObject.getEnableQueryRewrite())) {
            if (dbObject.getEnableQueryRewrite()) {
                sqlBuilder.line().append("ENABLE QUERY REWRITE");
            } else {
                sqlBuilder.line().append("DISABLE QUERY REWRITE");
            }
        }
        // 实时计算
        if (Objects.nonNull(dbObject.getEnableQueryComputation())) {
            if (dbObject.getEnableQueryComputation()) {
                sqlBuilder.line().append("ENABLE ON QUERY COMPUTATION");
            } else {
                sqlBuilder.line().append("DISABLE ON QUERY COMPUTATION");
            }
        }
        sqlBuilder.line().append("AS");
        // 此阶段获取queryStatement
        mySQLViewTemplate.generateQueryStatement(dbView, sqlBuilder);
        return sqlBuilder.toString();
    }

    private String getColumn(@NotNull String input) {
        Pattern pattern = Pattern.compile("(`[^`]+`)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return input;
    }

    private String getPrimary(@NotNull String input) {
        return input.replaceFirst("(?i)CONSTRAINT\\s*", "");
    }

    private String getFullyQualifiedTableName(@NotNull DBMView dbmView) {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();;
        if (StringUtils.isNotEmpty(dbmView.getSchemaName())) {
            sqlBuilder.identifier(dbmView.getSchemaName()).append(".");
        }
        sqlBuilder.identifier(dbmView.getName());
        return sqlBuilder.toString();
    }
}
