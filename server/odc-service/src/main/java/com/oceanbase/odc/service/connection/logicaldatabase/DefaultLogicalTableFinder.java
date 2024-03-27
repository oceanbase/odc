/*
 * Copyright (c) 2024 OceanBase.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTable;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/3/22 17:20
 * @Description: []
 */
@Slf4j
@Validated
public class DefaultLogicalTableFinder implements LogicalTableFinder {
    private ExecutorService executorService;

    @Override
    public List<LogicalTable> find(@NotEmpty List<Database> databases) {
        Map<Long, ConnectionConfig> id2DataSources = new HashMap<>();
        Map<ConnectionConfig, List<Database>> dataSource2Databases = new HashMap<>();
        for (Database database : databases) {
            Long dataSourceId = database.getDataSource().getId();
            ConnectionConfig connectionConfig = id2DataSources.computeIfAbsent(dataSourceId,
                k -> database.getDataSource());
            List<Database> groupedDatabases = dataSource2Databases.computeIfAbsent(connectionConfig,
                k -> new ArrayList<>());
            groupedDatabases.add(database);
        }

        executorService = Executors.newFixedThreadPool(dataSource2Databases.size());

        Set<ConnectionConfig> dataSources = dataSource2Databases.keySet();
        for (ConnectionConfig dataSource : dataSources) {
            List<Database> groupedDatabases = dataSource2Databases.get(dataSource);
            executorService.submit(() -> {
                List<String> tableNames = findTableNames(dataSource, groupedDatabases);
            });
        }
        return null;
    }

    private List<String> findTableNames(ConnectionConfig dataSource, List<Database> databases) {
        ConnectionSessionFactory connectionSessionFactory = new DefaultConnectSessionFactory(dataSource);
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSessionFactory.generateSession());
        return null;
    }
}
