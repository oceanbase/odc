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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.loaddata.model.LoadDataParameters;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.CreateScheduleReq;
import com.oceanbase.odc.service.schedule.model.LogicalDatabaseChangeParameters;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/6/25 17:01
 * @Descripition:
 */

@Slf4j
@Aspect
@Component
public class ScheduleChangePreprocessor implements InitializingBean {
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private List<Preprocessor> preprocessors;

    private final Map<ScheduleType, Preprocessor> type2Processor = new HashMap<>();

    public void process(ScheduleChangeParams params) {

        log.info("Process schedule change params, params={}", params);
        ScheduleType type;
        if (params.getOperationType() == OperationType.CREATE) {
            type = params.getCreateScheduleReq().getType();
        } else {
            type = scheduleService.nullSafeGetModelById(params.getScheduleId()).getType();
        }
        adaptScheduleChangeParams(params);
        if (type2Processor.containsKey(type)) {
            type2Processor.get(type).process(params);
        }
        log.info("Process schedule change params successfully, params={}", params);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        preprocessors.forEach(preprocessor -> {
            // Init schedule task processor.
            if (preprocessor.getClass().isAnnotationPresent(ScheduleTaskPreprocessor.class)) {
                ScheduleTaskPreprocessor annotation =
                        preprocessor.getClass().getAnnotation(ScheduleTaskPreprocessor.class);
                if (annotation.isEnabled()) {
                    if (type2Processor.containsKey(annotation.type())) {
                        throw new RuntimeException(
                                String.format("The processor has already been defined,type=%s", annotation.type()));
                    }
                    type2Processor.put(annotation.type(), preprocessor);
                }
            }
        });
    }

    private void adaptScheduleChangeParams(ScheduleChangeParams req) {
        Database srcDb = databaseService.detailSkipPermissionCheckForRead(getTargetDatabaseId(req));
        req.setProjectId(srcDb.getProject().getId());
        req.setProjectName(srcDb.getProject().getName());
        req.setConnectionId(srcDb.getDataSource().getId());
        req.setConnectionName(srcDb.getDataSource().getName());
        req.setEnvironmentId(srcDb.getEnvironment().getId());
        req.setEnvironmentName(srcDb.getEnvironment().getName());
        req.setDatabaseName(srcDb.getName());
        req.setDatabaseId(srcDb.getId());
    }

    private Long getTargetDatabaseId(ScheduleChangeParams params) {
        if (params.getOperationType() != OperationType.CREATE) {
            return scheduleService.nullSafeGetById(params.getScheduleId()).getDatabaseId();
        }
        CreateScheduleReq req = params.getCreateScheduleReq();
        switch (req.getType()) {
            case DATA_ARCHIVE: {
                DataArchiveParameters parameters = (DataArchiveParameters) req.getParameters();
                return parameters.getSourceDatabaseId();
            }
            case DATA_DELETE: {
                DataDeleteParameters parameters = (DataDeleteParameters) req.getParameters();
                return parameters.getDatabaseId();
            }
            case SQL_PLAN: {
                SqlPlanParameters parameters = (SqlPlanParameters) req.getParameters();
                return parameters.getDatabaseId();
            }
            case LOGICAL_DATABASE_CHANGE: {
                LogicalDatabaseChangeParameters parameters = (LogicalDatabaseChangeParameters) req.getParameters();
                return parameters.getDatabaseId();
            }
            case LOAD_DATA:
                LoadDataParameters parameters = (LoadDataParameters) req.getParameters();
                return parameters.getDatabaseId();
            default:
                throw new UnsupportedException();
        }
    }

}
