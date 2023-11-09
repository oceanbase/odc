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
package com.oceanbase.odc.service.schedule.job;

import java.util.List;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/9 14:46
 * @Descripition:
 */
@Slf4j
public class DataArchiveJob extends AbstractDlmJob {
    @Override
    public void execute(JobExecutionContext context) {

        jobThread = Thread.currentThread();

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();

        List<DlmTask> taskUnits = getTaskUnits(taskEntity);

        executeTask(taskEntity.getId(), taskUnits);
        TaskStatus taskStatus = getTaskStatus(taskUnits);
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);

        DataArchiveParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);

        if (taskStatus == TaskStatus.DONE && parameters.isDeleteAfterMigration()) {
            log.info("Start to create clear job,taskId={}", taskEntity.getId());
            scheduleService.dataArchiveDelete(Long.parseLong(taskEntity.getJobName()), taskEntity.getId());
            log.info("Clear job is created,");
        }
    }

    @Override
    public void initTask(DlmTask taskUnit) {
        super.initTask(taskUnit);
        createTargetTable(taskUnit);
    }


    /**
     * Create the table in the target database before migrating the data.
     */
    private void createTargetTable(DlmTask dlmTask) {

        if (dlmTask.getSourceDs().getDialectType() != dlmTask.getTargetDs().getDialectType()) {
            log.info("Data sources of different types do not currently support automatic creation of target tables.");
            return;
        }
        DefaultConnectSessionFactory sourceConnectionSessionFactory =
                new DefaultConnectSessionFactory(dlmTask.getSourceDs());
        ConnectionSession srcSession = sourceConnectionSessionFactory.generateSession();
        String tableDDL;
        try {
            DBSchemaAccessor sourceDsAccessor = DBSchemaAccessors.create(srcSession);
            tableDDL = sourceDsAccessor.getTableDDL(dlmTask.getSourceDs().getDefaultSchema(), dlmTask.getTableName());
        } finally {
            srcSession.expire();
        }

        DefaultConnectSessionFactory targetConnectionSessionFactory =
                new DefaultConnectSessionFactory(dlmTask.getTargetDs());
        ConnectionSession targetSession = targetConnectionSessionFactory.generateSession();
        try {
            DBSchemaAccessor targetDsAccessor = DBSchemaAccessors.create(targetSession);
            List<String> tableNames = targetDsAccessor.showTables(dlmTask.getTargetDs().getDefaultSchema());
            if (tableNames.contains(dlmTask.getTableName())) {
                log.info("Target table exist,tableName={}", dlmTask.getTableName());
                return;
            }
            log.info("Begin to create target table...");
            targetSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(tableDDL);
        } finally {
            targetSession.expire();
        }
    }

}
