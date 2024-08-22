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
package com.oceanbase.odc.service.schedule.processor;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ScheduleTaskPreprocessor(type = ScheduleType.SQL_PLAN)
public class SqlPlanPreprocessor implements Preprocessor {

    @Autowired
    private DatabaseService databaseService;

    @Override
    public void process(ScheduleChangeParams req) {
        log.info("database change pre process");
        if (req.getOperationType() == OperationType.CREATE || req.getOperationType() == OperationType.UPDATE) {
            SqlPlanParameters parameters = req.getOperationType() == OperationType.CREATE
                    ? (SqlPlanParameters) req.getCreateScheduleReq().getParameters()
                    : (SqlPlanParameters) req.getUpdateScheduleReq().getParameters();
            Database database = databaseService.detail(parameters.getDatabaseId());
            parameters.setDatabaseInfo(database);
        }
    }
}
