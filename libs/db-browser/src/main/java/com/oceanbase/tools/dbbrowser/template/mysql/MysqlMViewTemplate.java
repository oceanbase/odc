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

package com.oceanbase.tools.dbbrowser.template.mysql;

import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBMViewSyncPattern;
import com.oceanbase.tools.dbbrowser.model.DBMViewSyncSchedule;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.template.BaseViewTemplate;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 16:45
 * @since: 4.3.4
 */
public class MysqlMViewTemplate implements DBObjectTemplate<DBMView> {
   private BaseViewTemplate mySQLViewTemplate;
   private DBTableEditor dbTableEditor;

   public  MysqlMViewTemplate() {
        mySQLViewTemplate = new MySQLViewTemplate();
//        DBTableEditorFactory dbTableEditorFactory = new DBTableEditorFactory();
       dbTableEditor = DBBrowser.objectEditor().tableEditor()
           .setDbVersion("4.3.3")
           .setType("OB_MYSQL").create();

//       dbTableEditorFactory.setDbVersion("4.3.3");
//        dbTableEditorFactory.setType("OB_MYSQL");
//        dbTableEditor = dbTableEditorFactory.buildForOBMySQL();
    }

    @Override
    public String generateCreateObjectTemplate(DBMView dbObject) {
        DBTable dbTable = dbObject.generateDBTable();
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("create materialized view ")
            .append(dbTableEditor.getFullyQualifiedTableName(dbTable));
        boolean isFirstSentence = true;
        // 获取列构造
        if(Objects.nonNull(dbObject.getColumns())){
            for (DBTableColumn column : dbObject.getColumns()) {
                if(isFirstSentence){
                    sqlBuilder.append(" (").line();
                }else {
                    sqlBuilder.append(",").line();
                }
                isFirstSentence = false;
                sqlBuilder.append(getColumn(dbTableEditor.getColumnEditor().generateCreateDefinitionDDL(column)));
            }
            // 获取主键构造
            for (DBTableConstraint constraint : dbTableEditor. excludeUniqueConstraint(null, dbObject.getConstraints())) {
                if (!isFirstSentence) {
                    sqlBuilder.append(",").line();
                }
                isFirstSentence = false;
                sqlBuilder.append(dbTableEditor.getConstraintEditor().generateCreateDefinitionDDL(constraint));
            }
            sqlBuilder.line().append(") ");
        }
        // 构造物化视图并行度
        if (Objects.nonNull(dbObject.getParallelismDegree())&&dbObject.getParallelismDegree()>1) {
            sqlBuilder.line().append("PARALLEL ").append(dbObject.getParallelismDegree());
        }
        // 获取分区构造
        if (Objects.nonNull(dbObject.getPartition())) {
            sqlBuilder.line().append(dbTableEditor.getPartitionEditor().generateCreateDefinitionDDL(dbObject.getPartition()));
        }
        // 获取存储格式构造
        if (CollectionUtils.isNotEmpty(dbObject.getColumnGroups())) {
            sqlBuilder.line().append(" WITH COLUMN GROUP(")
                .append(dbObject.getColumnGroups().stream().map(DBColumnGroupElement::toString)
                    .collect(Collectors.joining(",")))
                .append(")");
        }
        // 物化视图刷新方式
        if(Objects.nonNull(dbObject.getSyncDataMethod())){
            sqlBuilder.line().append(dbObject.getSyncDataMethod().getName());
        }
        // 物化视图刷新模式
        if(CollectionUtils.isNotEmpty(dbObject.getSyncPatterns())){
            if(dbObject.getSyncPatterns().contains(DBMViewSyncPattern.ON_DEMAND))
            sqlBuilder.line().append("ON DEMAND");
        }
        if(Objects.nonNull(dbObject.getSyncSchedule())){
            DBMViewSyncSchedule syncSchedule = dbObject.getSyncSchedule();
            if(syncSchedule.getStartStrategy()== DBMViewSyncSchedule.StartStrategy.START_NOW){
                sqlBuilder.line().append("START WITH sysdate()");
                sqlBuilder.line().append("NEXT sysdate() + INTERVAL ").append(syncSchedule.getInterval()).append(" ").append(syncSchedule);
            }
        }
        // 查询改写
        if(dbObject.isEnableQueryRewrite()){
            sqlBuilder.line().append("ENABLE QUERY REWRITE");
        }else {
            sqlBuilder.line().append("DISABLE QUERY REWRITE");
        }
        // 实时计算
        if(dbObject.isEnableQueryComputation()){
            sqlBuilder.line().append("ENABLE ON QUERY COMPUTATION");
        }else {
            sqlBuilder.line().append("DISABLE ON QUERY COMPUTATION");
        }
        sqlBuilder.line().append("AS").line();
        // 此阶段获取queryStatement
        mySQLViewTemplate.generateQueryStatement(dbObject.generateDBView(), sqlBuilder);
        return sqlBuilder.toString();
    }

    private String getColumn(@NotNull String input){
        Pattern pattern = Pattern.compile("(`[^`]+`)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String extractedColumn = matcher.group(1);
            return extractedColumn;
        } else {
            return input;
        }
    }
}
