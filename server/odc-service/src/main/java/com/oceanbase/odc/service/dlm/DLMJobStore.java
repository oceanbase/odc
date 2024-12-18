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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.schedule.job.DLMJobReq;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.tools.migrator.common.dto.JobStatistic;
import com.oceanbase.tools.migrator.common.dto.TableSizeInfo;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;
import com.oceanbase.tools.migrator.common.element.PrimaryKey;
import com.oceanbase.tools.migrator.common.exception.JobException;
import com.oceanbase.tools.migrator.common.exception.JobSqlException;
import com.oceanbase.tools.migrator.common.meta.TableMeta;
import com.oceanbase.tools.migrator.core.IJobStore;
import com.oceanbase.tools.migrator.core.handler.genarator.GeneratorStatus;
import com.oceanbase.tools.migrator.core.handler.genarator.GeneratorType;
import com.oceanbase.tools.migrator.core.meta.ClusterMeta;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.core.meta.TaskMeta;
import com.oceanbase.tools.migrator.core.meta.TenantMeta;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/1/24 19:57
 * @Descripition:
 */
@Slf4j
public class DLMJobStore implements IJobStore {

    private DruidDataSource dataSource;
    private boolean enableBreakpointRecovery = false;
    private Map<String, DlmTableUnit> dlmTableUnits;
    private Map<String, String> jobParameters;

    public DLMJobStore(ConnectionConfig metaDBConfig) {

    }

    public void setDlmTableUnits(Map<String, DlmTableUnit> dlmTableUnits) {
        this.dlmTableUnits = dlmTableUnits;
    }

    public void destroy() {
        try {
            if (null != dataSource) {
                dataSource.close();
            }
        } catch (Exception e) {
            log.warn("Close meta datasource failed,errorMsg={}", e);
        }
    }

    @Override
    public TaskGenerator getTaskGenerator(String generatorId, String jobId) throws SQLException {
        if (enableBreakpointRecovery) {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "select * from dlm_task_generator where job_id = ?")) {
                ps.setString(1, jobId);
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    TaskGenerator taskGenerator = new TaskGenerator();
                    taskGenerator.setId(resultSet.getString("generator_id"));
                    taskGenerator.setGeneratorType(GeneratorType.valueOf(resultSet.getString("type")));
                    taskGenerator.setGeneratorStatus(GeneratorStatus.valueOf(resultSet.getString("status")));
                    taskGenerator.setJobId(jobId);
                    taskGenerator.setTaskCount(resultSet.getInt("task_count"));
                    taskGenerator
                            .setGeneratorSavePoint(PrimaryKey.valuesOf(resultSet.getString("primary_key_save_point")));
                    taskGenerator.setProcessedDataSize(resultSet.getLong("processed_row_count"));
                    taskGenerator.setProcessedDataSize(resultSet.getLong("processed_data_size"));
                    taskGenerator.setGeneratorPartitionSavepoint(resultSet.getString("partition_save_point"));
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
                ps.setString(6, GeneratorType.AUTO.name());
                ps.setLong(7, taskGenerator.getTaskCount());
                ps.setString(8, taskGenerator.getGeneratorSavePoint() == null ? ""
                        : taskGenerator.getGeneratorSavePoint().toSqlString());
                ps.setString(9, taskGenerator.getGeneratorPartitionSavepoint());
                if (ps.executeUpdate() == 1) {
                    log.info("Update task generator success.jobId={}", taskGenerator.getJobId());
                } else {
                    log.warn("Update task generator affect 0 row.jobId={}", taskGenerator.getJobId());
                }
            }
        }
    }

    @Override
    public void bindGeneratorToJob(String s, TaskGenerator taskGenerator) throws SQLException {

    }

    @Override
    public JobStatistic getJobStatistic(String s) throws JobException {
        return new JobStatistic();
    }

    @Override
    public void storeJobStatistic(JobMeta jobMeta) throws JobSqlException {
        dlmTableUnits.get(jobMeta.getJobId()).getStatistic().setProcessedRowCount(jobMeta.getJobStat().getRowCount());
        dlmTableUnits.get(jobMeta.getJobId()).getStatistic()
                .setProcessedRowsPerSecond(jobMeta.getJobStat().getAvgRowCount());

        dlmTableUnits.get(jobMeta.getJobId()).getStatistic().setReadRowCount(jobMeta.getJobStat().getReadRowCount());
        dlmTableUnits.get(jobMeta.getJobId()).getStatistic()
                .setReadRowsPerSecond(jobMeta.getJobStat().getAvgReadRowCount());
    }

    @Override
    public List<TaskMeta> getTaskMeta(JobMeta jobMeta) throws SQLException {
        if (enableBreakpointRecovery) {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "select * from dlm_task_unit where generator_id = ? AND status !='SUCCESS'")) {
                ps.setString(1, jobMeta.getGenerator().getId());
                ResultSet resultSet = ps.executeQuery();
                List<TaskMeta> taskMetas = new LinkedList<>();
                while (resultSet.next()) {
                    TaskMeta taskMeta = new TaskMeta();
                    taskMeta.setTaskIndex(resultSet.getLong("task_index"));
                    taskMeta.setJobMeta(jobMeta);
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
            log.info("start to store taskMeta:{}", taskMeta);
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
    public Long getAbnormalTaskIndex(String jobId) {
        if (enableBreakpointRecovery) {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "select count(1) from dlm_task_unit where job_id=? and (status != 'SUCCESS' or primary_key_cursor is null)")) {
                ps.setString(1, jobId);
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    long count = resultSet.getLong(1);
                    return count > 0 ? count : null;
                }
            } catch (Exception ignored) {
                log.warn("Get abnormal task failed.jobId={}", jobId);
            }
        }
        return null;
    }

    @Override
    public void updateTableSizeInfo(TableSizeInfo tableSizeInfo, long l) {

    }

    @Override
    public void updateLimiter(JobMeta jobMeta) {
        try {
            RateLimitConfiguration params;
            if (jobParameters.containsKey(JobParametersKeyConstants.DLM_RATE_LIMIT_CONFIG)) {
                params = JsonUtils.fromJson(
                        jobParameters.get(JobParametersKeyConstants.DLM_RATE_LIMIT_CONFIG),
                        RateLimitConfiguration.class);
            } else {
                DLMJobReq dlmJobReq = JsonUtils.fromJson(
                        jobParameters.get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                        DLMJobReq.class);
                params = dlmJobReq.getRateLimit();
            }
            if (params.getDataSizeLimit() != null) {
                setClusterLimitConfig(jobMeta.getSourceCluster(), params.getDataSizeLimit());
                setClusterLimitConfig(jobMeta.getTargetCluster(), params.getDataSizeLimit());
                setTenantLimitConfig(jobMeta.getSourceTenant(), params.getDataSizeLimit());
                setTenantLimitConfig(jobMeta.getTargetTenant(), params.getDataSizeLimit());
                log.info("Update rate limit success,dataSizeLimit={}", params.getDataSizeLimit());
            }
            if (params.getRowLimit() != null) {
                setTableLimitConfig(jobMeta.getTargetTableMeta(), params.getRowLimit());
                setTableLimitConfig(jobMeta.getSourceTableMeta(), params.getRowLimit());
                log.info("Update rate limit success,rowLimit={}", params.getRowLimit());
            }
        } catch (Exception e) {
            log.warn("Update rate limit failed,errorMsg={}", e.getMessage());
            setClusterLimitConfig(jobMeta.getSourceCluster(), 1024);
            setClusterLimitConfig(jobMeta.getTargetCluster(), 1024);
            setTenantLimitConfig(jobMeta.getSourceTenant(), 1024);
            setTenantLimitConfig(jobMeta.getTargetTenant(), 1024);
            setTableLimitConfig(jobMeta.getTargetTableMeta(), 1000);
            setTableLimitConfig(jobMeta.getSourceTableMeta(), 1000);
        }
    }

    public void setJobParameters(Map<String, String> jobParameters) {
        this.jobParameters = jobParameters;
    }

    private void setClusterLimitConfig(ClusterMeta clusterMeta, long dataSizeLimit) {
        clusterMeta.setReadSizeLimit(dataSizeLimit);
        clusterMeta.setWriteSizeLimit(dataSizeLimit);
        clusterMeta.setWriteUsedQuota(1);
        clusterMeta.setReadUsedQuota(1);
    }

    private void setTenantLimitConfig(TenantMeta tenantMeta, long dataSizeLimit) {
        tenantMeta.setReadSizeLimit(dataSizeLimit);
        tenantMeta.setWriteSizeLimit(dataSizeLimit);
        tenantMeta.setWriteUsedQuota(1);
        tenantMeta.setReadUsedQuota(1);
    }

    private void setTableLimitConfig(TableMeta tableMeta, int rowLimit) {
        tableMeta.setReadRowCountLimit(rowLimit);
        tableMeta.setWriteRowCountLimit(rowLimit);
    }
}
