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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.migrator.common.dto.JobStatistic;
import com.oceanbase.tools.migrator.common.dto.TableSizeInfo;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;
import com.oceanbase.tools.migrator.common.exception.JobException;
import com.oceanbase.tools.migrator.common.exception.JobSqlException;
import com.oceanbase.tools.migrator.common.exception.TaskGeneratorNotFoundException;
import com.oceanbase.tools.migrator.common.meta.TableMeta;
import com.oceanbase.tools.migrator.core.IJobStore;
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
public class CloudDLMJobStore implements IJobStore {

    private ConnectionConfig metaDBConfig;
    private ConnectionSession connectionSession;

    public CloudDLMJobStore(ConnectionConfig metaDBConfig) {
        this.metaDBConfig = metaDBConfig;
        initConnectionSession();
    }

    private void initConnectionSession() {
        connectionSession = new DefaultConnectSessionFactory(metaDBConfig).generateSession();
    }

    @Override
    public TaskGenerator getTaskGenerator(String s, String s1) throws TaskGeneratorNotFoundException, SQLException {
        return null;
    }

    @Override
    public void storeTaskGenerator(TaskGenerator taskGenerator) throws SQLException {

    }

    @Override
    public void bindGeneratorToJob(String s, TaskGenerator taskGenerator) throws SQLException {

    }

    @Override
    public JobStatistic getJobStatistic(String s) throws JobException {
        return null;
    }

    @Override
    public void storeJobStatistic(JobMeta jobMeta) throws JobSqlException {

    }

    @Override
    public List<TaskMeta> getTaskMeta(JobMeta jobMeta) throws SQLException {
        return null;
    }

    @Override
    public void storeTaskMeta(TaskMeta taskMeta) throws SQLException {

    }

    @Override
    public Long getAbnormalTaskIndex(String s) throws JobSqlException {
        return null;
    }

    @Override
    public void updateTableSizeInfo(TableSizeInfo tableSizeInfo, long l) {

    }

    @Override
    public void updateLimiter(JobMeta jobMeta) throws SQLException {
        try {
            SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                    ConnectionSessionConstants.BACKEND_DS_KEY);
            RateLimitConfiguration rateLimit = syncJdbcExecutor.execute(
                    (StatementCallback<RateLimitConfiguration>) statement -> {
                        ResultSet resultSet = statement.executeQuery(
                                String.format("select * from dlm_config_limiter_configuration where order_id = %s",
                                        DlmJobIdUtil.getJobName(jobMeta.getJobId())));
                        if (resultSet.next()) {
                            RateLimitConfiguration result = new RateLimitConfiguration();
                            result.setOrderId(resultSet.getLong("order_id"));
                            result.setBatchSize(resultSet.getInt("batch_size"));
                            result.setDataSizeLimit(resultSet.getLong("data_size_limit"));
                            result.setRowLimit(resultSet.getInt("row_limit"));
                            return result;
                        } else {
                            return null;
                        }

                    });
            if (rateLimit == null) {
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
}
