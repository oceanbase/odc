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
package com.oceanbase.odc.service.connection.logicaldatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.concurrent.Await;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTableEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableFinder;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/5/15 19:34
 * @Description: []
 */
public class LogicalTableCheckConsistencyTask implements Runnable {
    private final LogicalTableEntity table;
    private final LogicalTablePhysicalTableRepository relationRepository;
    private final DatabaseService databaseService;
    private final DatabaseRepository databaseRepository;

    public LogicalTableCheckConsistencyTask(LogicalTableEntity table,
            LogicalTablePhysicalTableRepository relationRepository, DatabaseService databaseService, DatabaseRepository databaseRepository) {
        this.table = table;
        this.relationRepository = relationRepository;
        this.databaseService = databaseService;
        this.databaseRepository = databaseRepository;
    }

    @Override
    public void run() {
        List<LogicalTablePhysicalTableEntity> relations =
                relationRepository.findByLogicalTableIdIn(Collections.singleton(table.getId()));

        Set<Long> databaseIds = relations.stream().map(LogicalTablePhysicalTableEntity::getPhysicalDatabaseId).collect(Collectors.toSet());
        List<Database> databases = databaseService.listDatabasesDetailsByIds(databaseIds);

        LogicalTableFinder finder = new LogicalTableFinder(databases);
        List<DataNode> dataNodes = finder.transferToDataNodes();





    }
}
