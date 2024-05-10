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

import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBPhysicalDBRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTableRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/5/10 11:41
 * @Description: []
 */

@Service
@Slf4j
public class LogicalDatabaseSyncManager {
    @Autowired
    @Qualifier("logicalTableExtractTaskExecutor")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private DatabaseRepository databaseRepository;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private LogicalDBPhysicalDBRepository dbRelationRepository;
    @Autowired
    private LogicalTablePhysicalTableRepository tableRelationRepository;
    @Autowired
    private LogicalTableRepository tableRepository;
    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    public void submitExtractLogicalTablesTask(@NotNull Database logicalDatabase) {
        doExecute(() -> executor
                .submit(new LogicalTableExtractTask(logicalDatabase, databaseRepository, dbRelationRepository,
                        databaseService, tableRepository, tableRelationRepository, jdbcLockRegistry)));
    }

    private Future<?> doExecute(Supplier<Future<?>> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            throw new BadRequestException("submit logical table extract task failed, ", ex);
        }
    }

}
