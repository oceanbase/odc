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
package com.oceanbase.odc.service.datasecurity;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnMeta;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo.ScanningTaskStatus;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.state.StatefulUuidStateIdGenerator;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/5/25 14:43
 */
@Slf4j
@Component
public class SensitiveColumnScanningTaskManager {

    private final SensitiveColumnScanningResultCache cache = SensitiveColumnScanningResultCache.getInstance();
    @Autowired
    @Qualifier("scanSensitiveColumnExecutor")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private StatefulUuidStateIdGenerator statefulUuidStateIdGenerator;

    public SensitiveColumnScanningTaskInfo start(List<Database> databases, List<SensitiveRule> rules,
            ConnectionConfig connectionConfig, Map<Long, List<SensitiveColumnMeta>> databaseId2SensitiveColumns) {
        ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        try {
            Long projectId = databases.get(0).getProject().getId();
            Verify.notNull(projectId, "projectId");
            DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
            Map<Database, Map<String, List<DBTableColumn>>> database2Table2ColumnsList = new HashMap<>();
            Map<Database, Map<String, List<DBTableColumn>>> database2View2ColumnsList = new HashMap<>();
            int objectCount = 0;
            for (Database database : databases) {
                Map<String, List<DBTableColumn>> table2Columns = accessor.listBasicTableColumns(database.getName());
                if (!table2Columns.isEmpty()) {
                    objectCount += table2Columns.keySet().size();
                    database2Table2ColumnsList.put(database, table2Columns);
                }
                Map<String, List<DBTableColumn>> view2Columns = accessor.listBasicViewColumns(database.getName());
                if (!view2Columns.isEmpty()) {
                    objectCount += view2Columns.keySet().size();
                    database2View2ColumnsList.put(database, view2Columns);
                }
            }
            SensitiveColumnScanningTaskInfo taskInfo = new SensitiveColumnScanningTaskInfo(
                    statefulUuidStateIdGenerator.generateStateId("SensitiveColumn"), projectId, objectCount);
            if (objectCount == 0) {
                taskInfo.setCompleteTime(new Date());
                taskInfo.setStatus(ScanningTaskStatus.SUCCESS);
            }
            cache.put(taskInfo.getTaskId(), taskInfo);
            Set<Database> targetDatabases =
                    Sets.union(database2Table2ColumnsList.keySet(), database2View2ColumnsList.keySet());
            for (Database database : targetDatabases) {
                List<SensitiveColumnMeta> sensitiveColumns = Collections.emptyList();
                if (databaseId2SensitiveColumns != null) {
                    sensitiveColumns =
                            databaseId2SensitiveColumns.getOrDefault(database.getId(), Collections.emptyList());
                }
                SensitiveColumnScanningTask subTask = new SensitiveColumnScanningTask(database, rules, taskInfo,
                        sensitiveColumns, database2Table2ColumnsList.getOrDefault(database, new HashMap<>()),
                        database2View2ColumnsList.getOrDefault(database, new HashMap<>()));
                try {
                    executor.submit(subTask);
                } catch (RejectedExecutionException e) {
                    taskInfo.setCompleteTime(new Date());
                    taskInfo.setStatus(ScanningTaskStatus.FAILED);
                    taskInfo.setErrorCode(ErrorCodes.Unexpected);
                    taskInfo.setErrorMsg(e.getLocalizedMessage());
                    break;
                }
            }
            return taskInfo;
        } finally {
            session.expire();
        }
    }

    public SensitiveColumnScanningTaskInfo get(String taskId) {
        PreConditions.validExists(ResourceType.ODC_SENSITIVE_COLUMN_SCANNING_TASK, "taskId", taskId,
                () -> cache.containsKey(taskId));
        SensitiveColumnScanningTaskInfo taskInfo = cache.get(taskId);
        if (Objects.nonNull(taskInfo.getErrorCode())) {
            String i18nErrorMessage =
                    taskInfo.getErrorCode().getLocalizedMessage(new Object[] {taskInfo.getErrorMsg()});
            taskInfo.setErrorMsg(i18nErrorMessage);
        }
        return taskInfo;
    }

}
