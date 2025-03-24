/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.tools.dbbrowser.template;


import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewRefreshMethod;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewRefreshSchedule;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.oceanbase.tools.dbbrowser.model.DBConstraintType.PRIMARY_KEY;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/23 22:31
 * @since: 4.3.4
 */
public abstract class BaseMViewTemplate implements DBObjectTemplate<DBMaterializedView> {
    private BaseViewTemplate viewTemplate;

    private DBTableConstraintEditor dbTableConstraintEditor;

    private DBTablePartitionEditor dbTablePartitionEditor;

    public BaseMViewTemplate(BaseViewTemplate viewTemplate,DBTableConstraintEditor constraintEditor,DBTablePartitionEditor partitionEditor){
        this.viewTemplate = viewTemplate;
        this.dbTableConstraintEditor= constraintEditor;
        this.dbTablePartitionEditor = partitionEditor;
    }

    @Override
    public final String generateCreateObjectTemplate(DBMaterializedView dbMView) {
        Validate.notBlank(dbMView.getName(), "Materialized view name can not be blank");
        DBView dbView = dbMView.generateDBView();
        viewTemplate.validOperations(dbView);
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("CREATE MATERIALIZED VIEW ")
            .append(getFullyQualifiedTableName(dbMView));
        // build sql about primary key
        if (CollectionUtils.isNotEmpty(dbMView.getConstraints())) {
            Validate.isTrue(
                dbMView.getConstraints().size() == 1 && dbMView.getConstraints().get(0).getType() == PRIMARY_KEY,
                "Only primary key is supported");
            DBTableConstraint dbTableConstraint = dbMView.getConstraints().get(0);
            sqlBuilder.append("(")
                .append(getPrimary(dbTableConstraintEditor.generateCreateDefinitionDDL(dbTableConstraint)))
                .append(")");
        }
        // build sql about parallelism degree
        if (Objects.nonNull(dbMView.getParallelismDegree()) && dbMView.getParallelismDegree() > 1) {
            sqlBuilder.line().append("PARALLEL ").append(dbMView.getParallelismDegree());
        }
        // build sql about partition
        if (Objects.nonNull(dbMView.getPartition())) {
            sqlBuilder.line()
                .append(dbTablePartitionEditor.generateCreateDefinitionDDL(dbMView.getPartition()));
        }
        // build sql about column group
        if (CollectionUtils.isNotEmpty(dbMView.getColumnGroups())) {
            sqlBuilder.line().append(" WITH COLUMN GROUP(")
                .append(dbMView.getColumnGroups().stream().map(DBColumnGroupElement::toString)
                    .collect(Collectors.joining(",")))
                .append(")");
        }
        // build sql about refresh method
        if (Objects.nonNull(dbMView.getRefreshMethod())) {
            sqlBuilder.line().append(dbMView.getRefreshMethod().getCreateName());
        }else{
            // an error will be reported when refresh method is not specified in OceanBase which version number is 4.3.5.1 and later
        sqlBuilder.line().append(DBMaterializedViewRefreshMethod.REFRESH_FORCE.getCreateName());
        }
        // build sql about refresh schedule
        fillRefreshSchedule(dbMView, sqlBuilder);
        // build sql about query rewrite
        if (Objects.nonNull(dbMView.getEnableQueryRewrite())) {
            if (dbMView.getEnableQueryRewrite()) {
                sqlBuilder.line().append("ENABLE QUERY REWRITE");
            } else {
                sqlBuilder.line().append("DISABLE QUERY REWRITE");
            }
        }
        // build sql about query computation
        if (Objects.nonNull(dbMView.getEnableQueryComputation())) {
            if (dbMView.getEnableQueryComputation()) {
                sqlBuilder.line().append("ENABLE ON QUERY COMPUTATION");
            } else {
                sqlBuilder.line().append("DISABLE ON QUERY COMPUTATION");
            }
        }
        sqlBuilder.line().append("AS");
        // build sql about query statement
        viewTemplate.generateQueryStatement(dbView, sqlBuilder);
        return sqlBuilder.toString();
    }

    private String getFullyQualifiedTableName(@NotNull DBMaterializedView DBMaterializedView) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(DBMaterializedView.getSchemaName())) {
            sqlBuilder.identifier(DBMaterializedView.getSchemaName()).append(".");
        }
        sqlBuilder.identifier(DBMaterializedView.getName());
        return sqlBuilder.toString();
    }

    private String getPrimary(@NotNull String input) {
        return input.replaceFirst("(?i)CONSTRAINT\\s*", "");
    }

    protected abstract SqlBuilder sqlBuilder();

    protected abstract void fillRefreshSchedule(DBMaterializedView dbMView, SqlBuilder sqlBuilder);

}
