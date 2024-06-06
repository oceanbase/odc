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

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.DlmTableUnitParameters;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.model.DlmTableUnitStatistic;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.dto.JobStatistic;
import com.oceanbase.tools.migrator.common.dto.TableSizeInfo;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;
import com.oceanbase.tools.migrator.common.element.PrimaryKey;
import com.oceanbase.tools.migrator.common.enums.JobType;
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
    private boolean enableBreakpointRecovery;

    public DLMJobStore(ConnectionConfig metaDBConfig) {
        this.dataSource = (DruidDataSource) new DruidDataSourceFactory(metaDBConfig).getDataSource();
        initEnableBreakpointRecovery();
    }

    public void destroy() {
        dataSource.close();
    }

    public List<DlmTableUnit> getDlmTableUnits(Long scheduleTaskId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement("select * from dlm_table_unit where schedule_task_id = ?")) {
            ps.setLong(1, scheduleTaskId);
            ResultSet resultSet = ps.executeQuery();
            List<DlmTableUnit> dlmTableUnits = new LinkedList<>();
            while (resultSet.next()) {
                DlmTableUnit dlmTableUnit = new DlmTableUnit();
                dlmTableUnit.setDlmTableUnitId(resultSet.getString("dlm_table_unit_id"));
                dlmTableUnit.setType(JobType.valueOf(resultSet.getString("type")));
                dlmTableUnit.setTableName(resultSet.getString("table_name"));
                dlmTableUnit.setTargetTableName(resultSet.getString("target_table_name"));
                dlmTableUnit.setFireTime(resultSet.getDate("fire_time"));
                dlmTableUnit.setStatus(
                        com.oceanbase.odc.core.shared.constant.TaskStatus.valueOf(resultSet.getString("status")));
                dlmTableUnit.setScheduleTaskId(resultSet.getLong("schedule_task_id"));
                dlmTableUnit.setSourceDatasourceInfo(JsonUtils.fromJson(resultSet.getString("source_datasource_info"),
                        DataSourceInfo.class));
                dlmTableUnit.setTargetDatasourceInfo(JsonUtils.fromJson(resultSet.getString("target_datasource_info"),
                        DataSourceInfo.class));
                dlmTableUnit.setParameters(
                        JsonUtils.fromJson(resultSet.getString("parameters"), DlmTableUnitParameters.class));
                dlmTableUnits.add(dlmTableUnit);
            }
            return dlmTableUnits;
        }
    }

    public void updateDlmTableUnitStatus(String dlmTableUnitId, TaskStatus status) throws SQLException {
        String sql = "UPDATE dlm_table_unit " +
                "SET status = ?, " +
                "start_time = CASE WHEN ? = 'RUNNING' AND start_time IS NULL THEN CURRENT_TIMESTAMP ELSE start_time END, "
                +
                "end_time = CASE WHEN ? IN ('CANCELED', 'DONE', 'FAILED') THEN CURRENT_TIMESTAMP ELSE end_time END " +
                "WHERE dlm_table_unit_id = ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, status.name());
            pstmt.setString(3, status.name());
            pstmt.setString(4, dlmTableUnitId);
            pstmt.executeUpdate();
        }
    }


    public void storeDlmTableUnit(List<DlmTableUnit> dlmTableUnits) throws SQLException {
        String sql = "INSERT INTO dlm_table_unit (schedule_task_id, dlm_table_unit_id, table_name, fire_time, " +
                "target_table_name, source_datasource_info, target_datasource_info, status, type, " +
                "parameters, statistic) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Try-with-resources statement ensures that each resource is closed at the end of the statement
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int count = 0;
            for (DlmTableUnit dlmTableUnit : dlmTableUnits) {
                ps.setLong(1, dlmTableUnit.getScheduleTaskId());
                ps.setString(2, dlmTableUnit.getDlmTableUnitId());
                ps.setString(3, dlmTableUnit.getTableName());
                ps.setDate(4, new Date(dlmTableUnit.getFireTime().getTime()));
                ps.setString(5, dlmTableUnit.getTargetTableName());
                ps.setString(6, JsonUtils.toJson(dlmTableUnit.getSourceDatasourceInfo()));
                ps.setString(7, JsonUtils.toJson(dlmTableUnit.getTargetDatasourceInfo()));
                ps.setString(8, dlmTableUnit.getStatus().name());
                ps.setString(9, dlmTableUnit.getType().name());
                ps.setString(10, JsonUtils.toJson(dlmTableUnit.getParameters()));
                ps.setString(11, JsonUtils.toJson(dlmTableUnit.getStatistic()));
                ps.addBatch();
                if (++count % 100 == 0 || count == dlmTableUnits.size()) {
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }
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
        DlmTableUnitStatistic statistic = new DlmTableUnitStatistic();
        statistic.setProcessedRowCount(jobMeta.getJobStat().getRowCount());
        statistic.setProcessedRowsPerSecond(jobMeta.getJobStat().getAvgRowCount());
        statistic.setReadRowCount(jobMeta.getJobStat().getReadRowCount());
        statistic.setReadRowsPerSecond(jobMeta.getJobStat().getAvgReadRowCount());
        String updateSql = "UPDATE dlm_table_unit SET statistic = ? WHERE dlm_table_unit_id = ?";

        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {

            preparedStatement.setString(1, JsonUtils.toJson(statistic));
            preparedStatement.setString(2, jobMeta.getJobId());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            log.warn("Update dlm job status failed,dlmTableUnitId={}", jobMeta.getJobId(), e);
        }
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
        return null;
    }

    @Override
    public void updateTableSizeInfo(TableSizeInfo tableSizeInfo, long l) {

    }

    @Override
    public void updateLimiter(JobMeta jobMeta) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "select * from dlm_config_limiter_configuration where order_id = ?")) {
            ps.setString(1, DlmJobIdUtil.getJobName(jobMeta.getJobId()));
            ResultSet resultSet = ps.executeQuery();
            RateLimitConfiguration rateLimit;
            if (resultSet.next()) {
                rateLimit = new RateLimitConfiguration();
                rateLimit.setOrderId(resultSet.getLong("order_id"));
                rateLimit.setBatchSize(resultSet.getInt("batch_size"));
                rateLimit.setDataSizeLimit(resultSet.getLong("data_size_limit"));
                rateLimit.setRowLimit(resultSet.getInt("row_limit"));
            } else {
                log.warn("RateLimitConfiguration not found,jobId={}", jobMeta.getJobId());
                return;
            }
            setClusterLimitConfig(jobMeta.getSourceCluster(), rateLimit.getDataSizeLimit());
            setClusterLimitConfig(jobMeta.getTargetCluster(), rateLimit.getDataSizeLimit());
            setTenantLimitConfig(jobMeta.getSourceTenant(), rateLimit.getDataSizeLimit());
            setTenantLimitConfig(jobMeta.getTargetTenant(), rateLimit.getDataSizeLimit());
            setTableLimitConfig(jobMeta.getSourceTableMeta(), rateLimit.getRowLimit());
            setTableLimitConfig(jobMeta.getTargetTableMeta(), rateLimit.getRowLimit());
            log.info("Update limiter success,jobId={},rateLimit={}",
                    jobMeta.getJobId(), rateLimit);
        } catch (Exception e) {
            log.warn("Update limiter failed,jobId={},error={}",
                    jobMeta.getJobId(), e);
        }

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

    private void initEnableBreakpointRecovery() {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement preparedStatement = conn.prepareStatement(
                        "select value from config_system_configuration where `key` = 'odc.task.dlm"
                                + ".support-breakpoint-recovery'")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                this.enableBreakpointRecovery = resultSet.getBoolean(1);
                log.info("The status of breakpoint recovery is {}", enableBreakpointRecovery);
                return;
            }
        } catch (Exception e) {
            log.warn("Load breakpoint recovery config failed!", e);
        }
        enableBreakpointRecovery = false;
    }
}
