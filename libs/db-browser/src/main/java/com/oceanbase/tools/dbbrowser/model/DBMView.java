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
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 11:03
 * @since: 4.3.4
 */
@Setter
@Getter
public class DBMView implements DBObject {
    private String mVName;
    // if null, use defaultSchemaName in current connection
    private String schemaName;
    private String ddl;
    // 刷新方式
    private DBMViewSyncDataMethod syncDataMethod;
    // 刷新并行度
    private Integer parallelismDegree;
    // 按需刷新
    private boolean onDemand = true;
    // 刷新计划
    private DBMViewSyncSchedule syncSchedule;
    // 查询改写
    private boolean enableQueryRewrite;
    // 实时计算
    private boolean enableQueryComputation;

    /**
     * 复用视图，用于构造query statement
     */

    // 基表
    private List<DBView.DBViewUnit> viewUnits = new ArrayList<>();
    // 列
    private List<DBViewColumn> createColumns;

    private List<String> operations = new ArrayList<>();

    /**
     * 复用表
     */

    private List<DBTableColumn> columns;
    // 主键约束
    private List<DBTableConstraint> constraints;
    // 分区
    private DBTablePartition partition;
    // 存储格式
    private List<DBColumnGroupElement> columnGroups;


    @Override
    public String name() {
        return this.mVName;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.MATERIALIZED_VIEW;
    }

    public DBTable generateDBTable() {
        DBTable dbTable = new DBTable();
        dbTable.setName(mVName);
        dbTable.setColumns(columns);
        dbTable.setSchemaName(schemaName);
        dbTable.setPartition(partition);
        dbTable.setConstraints(constraints);
        return dbTable;
    }

    public DBView generateDBView() {
        DBView dbView = new DBView();
        dbView.setViewName(mVName);
        dbView.setSchemaName(schemaName);
        dbView.setViewUnits(viewUnits);
        dbView.setCreateColumns(createColumns);
        dbView.setOperations(operations);
        return dbView;
    }
}
