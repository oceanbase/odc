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
package com.oceanbase.tools.dbbrowser.template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.DBView.DBViewUnit;
import com.oceanbase.tools.dbbrowser.model.DBViewColumn;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BaseViewTemplate}
 *
 * @author yh263208
 * @date 2023-02-23 17:33
 * @since db-browser_1.0.0-SNAPSHOT
 */
@Slf4j
public abstract class BaseViewTemplate implements DBObjectTemplate<DBView> {

    protected abstract String preHandle(String str);

    protected abstract SqlBuilder sqlBuilder();

    protected abstract String doGenerateCreateObjectTemplate(SqlBuilder sqlBuilder, DBView dbObject);

    @Override
    public String generateCreateObjectTemplate(@NotNull DBView dbObject) {
        Validate.notBlank(dbObject.getViewName(), "View name can not be blank");
        Validate.isTrue(dbObject.getOperations() == null
                || dbObject.getViewUnits() != null,
                "Unable to calculate while operation set but table not set");
        Validate.isTrue(dbObject.getOperations() != null
                || dbObject.getViewUnits() == null
                || dbObject.getViewUnits().size() == 1,
                "Unable to calculate while operation not set but table size not 1");
        Validate.isTrue(dbObject.getOperations() == null
                || dbObject.getViewUnits() == null
                || dbObject.getOperations().size() == 0 && dbObject.getViewUnits().size() == 0
                || dbObject.getOperations().size() == dbObject.getViewUnits().size() - 1,
                "Unable to calculate, operationSize<>tableSize-1");
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append(preHandle("create or replace view "))
                .identifier(dbObject.getViewName())
                .append(preHandle(" as"));
        ViewCreateParameters params = new ViewCreateParameters(dbObject);
        params.getSubParameters().forEach(p -> {
            if (Objects.isNull(p.getViewUnits()) && Objects.isNull(p.getColumns())) {
                // here we will only get one operation
                sqlBuilder.append("\n").append(preHandle(p.getOperations().get(0))).space();
                return;
            }
            handleQuery(sqlBuilder, p);
        });
        return doGenerateCreateObjectTemplate(sqlBuilder, dbObject);
    }

    private void handleQuery(SqlBuilder sqlBuilder, ViewCreateSubParameters subParam) {
        sqlBuilder.append("\n").append(preHandle("select"));
        List<DBViewColumn> columns = subParam.getColumns();
        if (CollectionUtils.isEmpty(columns)) {
            sqlBuilder.space().append("\n\t").append("*");
        } else {
            for (int i = 0; i < columns.size(); i++) {
                DBViewColumn column = subParam.getColumns().get(i);
                sqlBuilder.append("\n\t");
                handleColumnName(sqlBuilder, column, i, subParam);
                if (i < columns.size() - 1) {
                    sqlBuilder.append(",");
                }
            }
        }
        sqlBuilder.append(preHandle("\nfrom"));
        handleJoin(sqlBuilder, subParam);
    }

    private void handleColumnName(SqlBuilder sqlBuilder, DBViewColumn currentColumn, int index,
            ViewCreateSubParameters subParam) {
        String currentName = currentColumn.getColumnName();
        String currentAlias = currentColumn.getAliasName();
        if (Objects.isNull(currentColumn.getDbName()) || Objects.isNull(currentColumn.getTableName())) {
            // 自定义列不作特殊处理，需要调用端自行保证正确性
            sqlBuilder.append(currentName);
            if (StringUtils.isNotEmpty(currentAlias)) {
                sqlBuilder.append(preHandle(" as ")).append(currentAlias);
            }
            return;
        }
        String tableAlias = currentColumn.getTableAliasName();
        if (StringUtils.isNotEmpty(tableAlias)) {
            // table alias name will be added as column prefix if exist
            sqlBuilder.append(tableAlias)
                    .append(".")
                    .identifier(currentName);
            if (StringUtils.isNotEmpty(currentAlias)) {
                sqlBuilder.append(preHandle(" as ")).append(currentAlias);
            }
            return;
        }
        // if there is no table alias, check if exists duplicate column
        boolean duplicateAlias = false;
        for (int i = 0; i < subParam.getColumns().size(); i++) {
            if (i == index) {
                continue;
            }
            DBViewColumn column = subParam.getColumns().get(i);
            if (StringUtils.isNotEmpty(currentAlias) && currentAlias.equals(column.getAliasName())) {
                duplicateAlias = true;
            }
        }
        SqlBuilder prefixBuilder = sqlBuilder();
        if (!subParam.getParent().isSingleSchema()) {
            prefixBuilder.identifier(currentColumn.getDbName()).append(".");
        }
        prefixBuilder.identifier(currentColumn.getTableName());
        if (!subParam.getParent().isSingleTable()) {
            sqlBuilder.append(prefixBuilder.toString()).append(".");
        }
        sqlBuilder.identifier(currentName);
        if (StringUtils.isNotEmpty(currentAlias)) {
            sqlBuilder.append(preHandle(" as "));
            if (duplicateAlias) {
                sqlBuilder.append(prefixBuilder.toString()).append(".");
            }
            sqlBuilder.append(currentAlias);
        }
    }

    private void handleJoin(SqlBuilder sqlBuilder, ViewCreateSubParameters subParam) {
        // construct join part
        boolean hasCommaOperation = false;
        List<DBViewUnit> units = subParam.getViewUnits();
        sqlBuilder.append("\n\t");
        for (int i = 0; i < units.size(); i++) {
            DBViewUnit currentUnit = units.get(i);
            sqlBuilder.identifier(currentUnit.getDbName())
                    .append(".")
                    .identifier(currentUnit.getTableName());
            if (currentUnit.getTableAliasName() != null) {
                sqlBuilder.space().append(currentUnit.getTableAliasName());
            }
            if (i > 0) {
                String preOperation = subParam.getOperations().get(i - 1);
                if (",".equals(preOperation)) {
                    // , operation correspond to where at the end of sub sentence
                    hasCommaOperation = true;
                } else {
                    // other join operation correspond to on
                    sqlBuilder.append(preHandle(" on"))
                            .append(" /* TODO enter attribute to join on here */");
                }
            }
            if (i < units.size() - 1) {
                String currentOperation = subParam.getOperations().get(i);
                if (!",".equals(currentOperation)) {
                    sqlBuilder.append("\n\t");
                }
                sqlBuilder.append(preHandle(currentOperation)).space();
            }
        }
        if (!hasCommaOperation) {
            return;
        }
        sqlBuilder.append(preHandle("\nwhere"))
                .append(" /* TODO enter condition clause for where */");
    }

    @Setter
    @Getter
    private static class ViewCreateSubParameters {
        private List<DBViewUnit> viewUnits;
        private List<DBViewColumn> columns;
        private List<String> operations;
        private final ViewCreateParameters parent;

        public ViewCreateSubParameters(ViewCreateParameters parent) {
            this.parent = parent;
        }

        public void addOperation(String operation) {
            if (Objects.isNull(operations)) {
                operations = new ArrayList<>();
            }
            operations.add(operation);
        }
    }

    @Getter
    @Setter
    private static class ViewCreateParameters {
        private List<ViewCreateSubParameters> subParameters;
        private Set<String> tableSet;
        private Set<String> schemaSet;

        public ViewCreateParameters(DBView view) {
            subParameters = new ArrayList<>();
            tableSet = new HashSet<>();
            schemaSet = new HashSet<>();
            transform(view);
        }

        public boolean isSingleSchema() {
            return schemaSet.size() == 1;
        }

        public boolean isSingleTable() {
            return tableSet.size() == 1;
        }

        private void transform(DBView view) {
            List<DBViewUnit> viewUnits = view.getViewUnits();
            List<String> operations = view.getOperations();
            int unitIndex = 0;
            int operationIndex = 0;
            while (operationIndex < operations.size()) {
                String currentOperation = operations.get(operationIndex);
                if (!isJoin(currentOperation)) {
                    // collect to the last join
                    collectColumns(view, unitIndex, operationIndex);
                    // collect current operation which is not join
                    ViewCreateSubParameters currentSubParam = new ViewCreateSubParameters(this);
                    currentSubParam.addOperation(currentOperation);
                    subParameters.add(currentSubParam);

                    unitIndex = operationIndex + 1;
                }
                operationIndex++;
            }
            if (unitIndex < viewUnits.size()) {
                collectColumns(view, unitIndex, operationIndex);
            }
        }

        private boolean isJoin(String operation) {
            // , means inner join
            return StringUtils.containsAnyIgnoreCase(operation, "join") || ",".equals(operation);
        }

        private void collectColumns(DBView view, int unitIndex, int operationIndex) {
            ViewCreateSubParameters subParam = new ViewCreateSubParameters(this);
            List<DBViewUnit> dbViewUnits = view.getViewUnits().subList(unitIndex, operationIndex + 1);
            subParam.setViewUnits(dbViewUnits);
            List<String> operationSub = view.getOperations().subList(unitIndex, operationIndex);
            subParam.setOperations(operationSub);
            // to distinguish whether current column belongs to current bucket
            Set<String> localTableSet = new HashSet<>();
            for (DBViewUnit unit : dbViewUnits) {
                localTableSet.add(unit.getDbName() + "." + unit.getTableName());
                schemaSet.add(unit.getDbName());
            }
            List<DBViewColumn> columns = new ArrayList<>();
            if (!Objects.isNull(view.getCreateColumns())) {
                for (DBViewColumn column : view.getCreateColumns()) {
                    if (Objects.isNull(column.getDbName()) || Objects.isNull(column.getTableName())) {
                        // if it is a custom column, we need to add it in with no condition
                        columns.add(column);
                    }
                    if (localTableSet.contains(column.getDbName() + "." + column.getTableName())) {
                        // if it's dbname && tablename match, we need to add it in
                        columns.add(column);
                    }
                }
            }
            subParam.setColumns(columns);
            tableSet.addAll(localTableSet);
            subParameters.add(subParam);
        }
    }

}
