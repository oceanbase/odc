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
package com.oceanbase.tools.dbbrowser.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 11:03
 * @since: 4.3.4
 */
@Setter
@Getter
public class DBMaterializedView implements DBObject {
    /**
     * 所属 schema，API 层面不可见
     */
    @JsonIgnore
    @ToString.Exclude
    private DBSchema schema;
    @NotEmpty
    private String name;
    // if null, use defaultSchemaName in current connection
    private String schemaName;
    private String ddl;
    private DBMaterializedViewRefreshMethod refreshMethod;
    private Long parallelismDegree;
    private DBMaterializedViewRefreshSchedule refreshSchedule;
    private Boolean enableQueryRewrite;
    private Boolean enableQueryComputation;
    private DBMaterializedViewRefreshMethod lastRefreshType;
    private Date lastRefreshStartTime;
    private Date lastRefreshEndTime;
    /**
     * reuse properties in {@link DBView} to construct query statements
     */
    private List<DBView.DBViewUnit> viewUnits = new ArrayList<>();
    private List<DBViewColumn> createColumns;
    private List<String> operations = new ArrayList<>();

    /**
     * reuse properties in {@link DBTable}
     */
    private List<DBTableColumn> columns;
    private List<DBTableIndex> indexes;
    private List<DBTableConstraint> constraints;
    private DBTablePartition partition;
    private List<DBColumnGroupElement> columnGroups;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.MATERIALIZED_VIEW;
    }

    public DBView generateDBView() {
        DBView dbView = new DBView();
        dbView.setViewName(name);
        dbView.setSchemaName(schemaName);
        dbView.setViewUnits(viewUnits);
        dbView.setCreateColumns(createColumns);
        dbView.setOperations(operations);
        return dbView;
    }

    public DBTable generateDBTable() {
        DBTable dbTable = new DBTable();
        dbTable.setName(name);
        dbTable.setSchemaName(schemaName);
        dbTable.setColumns(columns);
        dbTable.setIndexes(indexes);
        dbTable.setConstraints(constraints);
        dbTable.setPartition(partition);
        return dbTable;
    }

}
