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
package com.oceanbase.odc.service.task.executor.task;

import java.sql.Connection;
import java.sql.ResultSet;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.dlm.CloudDLMJobStore;
import com.oceanbase.odc.service.dlm.DLMJobFactory;
import com.oceanbase.odc.service.schedule.job.DLMJobParameters;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.migrator.common.enums.DataBaseType;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.datasource.DataSourceAdapter;
import com.oceanbase.tools.migrator.datasource.DataSourceFactory;
import com.oceanbase.tools.migrator.job.Job;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/1/24 11:09
 * @Descripition:
 */

@Slf4j
public class DataArchiveTask extends BaseTask<Boolean> {

    private DLMJobFactory jobFactory;
    private boolean isFinish = false;
    private double progress = 0.0;
    private Job job;

    @Override
    protected void doInit(JobContext context) {
        jobFactory = new DLMJobFactory(new CloudDLMJobStore(JobUtils.getMetaDBConnectionConfig()));
        log.info("Init data-archive job env succeed,jobIdentity={}", context.getJobIdentity());
    }

    @Override
    protected boolean doStart(JobContext context) throws Exception {

        String taskParameters = context.getJobParameters().get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON);
        DLMJobParameters parameters = JsonUtils.fromJson(taskParameters,
                DLMJobParameters.class);

        for (int tableIndex = 0; tableIndex < parameters.getTables().size(); tableIndex++) {
            if (getStatus().isTerminated()) {
                log.info("Job is terminated,jobIdentity={}", context.getJobIdentity());
                break;
            }
            if (parameters.getJobType() == JobType.MIGRATE) {
                syncTable(tableIndex, parameters);
            }
            try {
                job = jobFactory.createJob(tableIndex, parameters);
                log.info("Init {} job succeed,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                log.info("{} job start,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                job.run();
                log.info("{} job finished,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
            } catch (Throwable e) {
                log.error("{} job failed,DLMJobId={},errorMsg={}", job.getJobMeta().getJobType(),
                        job.getJobMeta().getJobId(),
                        e);
            }
            progress = (tableIndex + 1.0) / parameters.getTables().size();
        }
        isFinish = true;
        return true;
    }

    private void syncTable(int tableIndex, DLMJobParameters parameters) throws Exception {
        DataSourceAdapter sourceDataSource = DataSourceFactory.getDataSource(parameters.getSourceDs());
        if (sourceDataSource.getDataBaseType().isOracle()) {
            log.info("Unsupported sync table construct for Oracle,databaseType={}", sourceDataSource.getDataBaseType());
            return;
        }
        DataSourceAdapter targetDataSource = DataSourceFactory.getDataSource(parameters.getTargetDs());
        if (parameters.getSourceDs().getDatabaseType() != parameters.getTargetDs().getDatabaseType()) {
            log.info("Data sources of different types do not currently support automatic creation of target tables.");
            return;
        }
        if (checkTargetTableExist(parameters.getTables().get(tableIndex).getTargetTableName(), targetDataSource)) {
            log.info("Target table exists.");
            // TODO sync the table structure if it exists.
            return;
        }
        log.info("Target table does not exists,tableName={}",
                parameters.getTables().get(tableIndex).getTargetTableName());
        String createTableDDL;
        try (Connection connection = sourceDataSource.getConnectionReadOnly()) {
            ResultSet resultSet = connection.prepareStatement(getCreateTableDDL(sourceDataSource.getSchema(),
                    parameters.getTables().get(tableIndex).getTableName(), sourceDataSource.getDataBaseType()))
                    .executeQuery();
            if (resultSet.next()) {
                createTableDDL = resultSet.getString(2);
            } else {
                log.info("Get create target table ddl failed,resultSet is null.");
                return;
            }
            log.info("Get Create table ddl finish, sql={}", createTableDDL);
        } catch (Exception ex) {
            log.warn("Get create target table ddl failed!", ex);
            return;
        }
        try (Connection connection = targetDataSource.getConnectionForWrite()) {
            boolean result = connection.prepareStatement(createTableDDL).execute();
            log.info("Create target table finish, result={}", result);
        }
    }

    private boolean checkTargetTableExist(String tableName, DataSourceAdapter dataSourceAdapter) {
        try (Connection connection = dataSourceAdapter.getConnectionReadOnly()) {
            ResultSet resultSet = connection.prepareStatement(String.format(
                    "SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema='%s'",
                    dataSourceAdapter.getDataSourceInfo().getDatabaseName())).executeQuery();
            while (resultSet.next()) {
                if (resultSet.getString(1).equals(tableName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private static String getCreateTableDDL(String schemaName, String tableName, DataBaseType dbType) {
        SqlBuilder sb = dbType == DataBaseType.OB_MYSQL || dbType == DataBaseType.MYSQL ? new MySQLSqlBuilder()
                : new OracleSqlBuilder();
        sb.append("SHOW CREATE TABLE ");
        sb.identifier(schemaName);
        sb.append(".");
        sb.identifier(tableName);
        return sb.toString();
    }

    @Override
    protected void doStop() throws Exception {
        job.getJobMeta().setToStop(true);
    }

    @Override
    protected void doClose() throws Exception {

    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public Boolean getTaskResult() {
        return isFinish;
    }
}
