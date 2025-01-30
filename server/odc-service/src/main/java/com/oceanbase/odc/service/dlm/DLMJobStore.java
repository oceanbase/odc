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
package com.oceanbase.odc.service.dlm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.tools.migrator.common.dto.JobStatistic;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;
import com.oceanbase.tools.migrator.common.element.PrimaryKey;
import com.oceanbase.tools.migrator.core.handler.genarator.GeneratorStatus;
import com.oceanbase.tools.migrator.core.meta.TaskMeta;
import com.oceanbase.tools.migrator.core.store.IJobStore;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/1/24 19:57
 * @Descripition:
 */
@Slf4j
public class DLMJobStore implements IJobStore {

    private DruidDataSource dataSource;
    private boolean enableBreakpointRecovery = true;
    @Setter
    private DlmTableUnit dlmTableUnit;

    public DLMJobStore(ConnectionConfig metaDBConfig) {
        try {
            DruidDataSourceFactory druidDataSourceFactory = new DruidDataSourceFactory(metaDBConfig);
            dataSource = (DruidDataSource) druidDataSourceFactory.getDataSource();
        } catch (Exception e) {
            log.warn("Failed to connect to the meta database; closing save point.");
            enableBreakpointRecovery = false;
        }

    }

    public void destroy() {
        if (dataSource == null) {
            return;
        }
        try {
            if (null != dataSource) {
                dataSource.close();
            }
        } catch (Exception e) {
            log.warn("Close meta datasource failed,errorMsg={}", e);
        }
    }

    @Override
    public TaskGenerator getTaskGenerator(String jobId) throws SQLException {
        if (enableBreakpointRecovery) {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "select * from dlm_task_generator where job_id = ?")) {
                ps.setString(1, jobId);
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    TaskGenerator taskGenerator = new TaskGenerator();
                    taskGenerator.setId(resultSet.getString("generator_id"));
                    taskGenerator.setGeneratorStatus(GeneratorStatus.valueOf(resultSet.getString("status")));
                    taskGenerator.setJobId(jobId);
                    taskGenerator.setTaskCount(resultSet.getInt("task_count"));
                    taskGenerator
                            .setPrimaryKeySavePoint(PrimaryKey.valuesOf(resultSet.getString("primary_key_save_point")));
                    taskGenerator.setProcessedDataSize(resultSet.getLong("processed_row_count"));
                    taskGenerator.setProcessedDataSize(resultSet.getLong("processed_data_size"));
                    taskGenerator.setPartitionSavePoint(resultSet.getString("partition_save_point"));
                    log.info("Load task generator success.jobId={}", jobId);
                    return taskGenerator;
                }
            }
        }
        log.info("Load task generator failed.jobId={}", jobId);
        return null;
    }

    @Override
    public void storeTaskGenerator(TaskGenerator taskGenerator) throws SQLException {
        taskGenerator.getPartName2MaxKey()
                .forEach((k, v) -> dlmTableUnit.getStatistic().getPartName2MaxKey().put(k, v.getSqlString()));
        taskGenerator.getPartName2MinKey()
                .forEach((k, v) -> dlmTableUnit.getStatistic().getPartName2MinKey().put(k, v.getSqlString()));
        if (taskGenerator.getGlobalMaxKey() != null) {
            dlmTableUnit.getStatistic().setGlobalMaxKey(taskGenerator.getGlobalMaxKey().getSqlString());
        }
        if (taskGenerator.getGlobalMinKey() != null) {
            dlmTableUnit.getStatistic().setGlobalMinKey(taskGenerator.getGlobalMinKey().getSqlString());
        }
        if (enableBreakpointRecovery) {
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO dlm_task_generator ");
            sb.append(
                    "(generator_id,job_id,processed_data_size,processed_row_count,status,type,task_count,primary_key_save_point,partition_save_point)");
            sb.append(" VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE ");
            sb.append(
                    "status=values(status),task_count=values(task_count),partition_save_point=values(partition_save_point),");
            sb.append(
                    "processed_row_count=values(processed_row_count),processed_data_size=values(processed_data_size),primary_key_save_point=values(primary_key_save_point)");
            log.info("start to store task generator:{}", taskGenerator);
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                ps.setString(1, taskGenerator.getId());
                ps.setString(2, taskGenerator.getJobId());
                ps.setLong(3, taskGenerator.getProcessedDataSize());
                ps.setLong(4, taskGenerator.getProcessedRowCount());
                ps.setString(5, taskGenerator.getGeneratorStatus().name());
                ps.setString(6, "");
                ps.setLong(7, taskGenerator.getTaskCount());
                ps.setString(8, taskGenerator.getPrimaryKeySavePoint() == null ? ""
                        : taskGenerator.getPrimaryKeySavePoint().toSqlString());
                ps.setString(9, taskGenerator.getPartitionSavePoint());
                if (ps.executeUpdate() == 1) {
                    log.info("Update task generator success.jobId={}", taskGenerator.getJobId());
                } else {
                    log.warn("Update task generator affect 0 row.jobId={}", taskGenerator.getJobId());
                }
            }
        }
    }

    @Override
    public JobStatistic getJobStatistic(String s) throws SQLException {
        return new JobStatistic();
    }

    @Override
    public void storeJobStatistic(JobStatistic jobStatistic) throws SQLException {
        dlmTableUnit.getStatistic()
                .setProcessedRowCount(jobStatistic.getRowCount().get());
        dlmTableUnit.getStatistic()
                .setProcessedRowsPerSecond(jobStatistic.getRowCountPerSeconds());

        dlmTableUnit.getStatistic().setReadRowCount(jobStatistic.getReadRowCount().get());
        dlmTableUnit.getStatistic()
                .setReadRowsPerSecond(jobStatistic.getReadRowCountPerSeconds());
    }

    @Override
    public List<TaskMeta> loadUnfinishedTask(String generatorId) throws SQLException {
        if (enableBreakpointRecovery) {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "select * from dlm_task_unit where generator_id = ? AND status !='SUCCESS'")) {
                ps.setString(1, generatorId);
                ResultSet resultSet = ps.executeQuery();
                List<TaskMeta> taskMetas = new LinkedList<>();
                while (resultSet.next()) {
                    TaskMeta taskMeta = new TaskMeta();
                    taskMeta.setTaskIndex(resultSet.getLong("task_index"));
                    taskMeta.setGeneratorId(resultSet.getString("generator_id"));
                    taskMeta.setTaskStatus(com.oceanbase.tools.migrator.common.enums.TaskStatus
                            .valueOf(resultSet.getString("status")));
                    taskMeta.setMinPrimaryKey(PrimaryKey.valuesOf(resultSet.getString("lower_bound_primary_key")));
                    taskMeta.setMaxPrimaryKey(PrimaryKey.valuesOf(resultSet.getString("upper_bound_primary_key")));
                    taskMeta.setCursorPrimaryKey(PrimaryKey.valuesOf(resultSet.getString("primary_key_cursor")));
                    taskMeta.setPartitionName(resultSet.getString("partition_name"));
                    taskMetas.add(taskMeta);
                }
                log.info("Load history task units success,count={}", taskMetas.size());
                return taskMetas;
            }
        }
        return null;
    }

    @Override
    public void storeTaskMeta(TaskMeta taskMeta) throws SQLException {
        if (enableBreakpointRecovery) {
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO dlm_task_unit ");
            sb.append(
                    "(task_index,job_id,generator_id,status,lower_bound_primary_key,upper_bound_primary_key,primary_key_cursor,partition_name)");
            sb.append(" VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE ");
            sb.append(
                    "status=values(status),partition_name=values(partition_name),lower_bound_primary_key=values(lower_bound_primary_key),");
            sb.append(
                    "upper_bound_primary_key=values(upper_bound_primary_key),primary_key_cursor=values(primary_key_cursor)");
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                ps.setLong(1, taskMeta.getTaskIndex());
                ps.setString(2, taskMeta.getJobMeta().getJobId());
                ps.setString(3, taskMeta.getGeneratorId());
                ps.setString(4, taskMeta.getTaskStatus().name());
                ps.setString(5, taskMeta.getMinPrimaryKey() == null ? "" : taskMeta.getMinPrimaryKey().toSqlString());
                ps.setString(6, taskMeta.getMaxPrimaryKey() == null ? "" : taskMeta.getMaxPrimaryKey().toSqlString());
                ps.setString(7,
                        taskMeta.getCursorPrimaryKey() == null ? "" : taskMeta.getCursorPrimaryKey().toSqlString());
                ps.setString(8, taskMeta.getPartitionName());
                if (ps.executeUpdate() == 1) {
                    log.info("Update task meta success.jobId={}", taskMeta.getJobMeta().getJobId());
                } else {
                    log.warn("Update task meta affect 0 row.jobId={}", taskMeta.getJobMeta().getJobId());
                }
            }
        }
    }

    @Override
    public long getAbnormalTaskCount(String jobId) {
        long count = 0;
        if (enableBreakpointRecovery) {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "select count(1) from dlm_task_unit where job_id=? and (status != 'SUCCESS' or primary_key_cursor is null)")) {
                ps.setString(1, jobId);
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    count = resultSet.getLong(1);
                }
            } catch (Exception ignored) {
                log.warn("Get abnormal task failed.jobId={}", jobId);
            }
        }
        return count;
    }


}
