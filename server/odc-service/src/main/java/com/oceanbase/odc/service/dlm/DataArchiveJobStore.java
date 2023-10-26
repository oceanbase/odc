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

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
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
 * @Date: 2023/5/8 19:27
 * @Descripition: TODO Store runtime data and use it to resume execution from a breakpoint.
 */
@Component
@Slf4j
public class DataArchiveJobStore implements IJobStore {
    @Autowired
    private DlmLimiterService limiterService;

    @Override
    public TaskGenerator getTaskGenerator(String generatorId, String jobId)
            throws TaskGeneratorNotFoundException, SQLException {
        return null;
    }

    @Override
    public void storeTaskGenerator(TaskGenerator taskGenerator) throws SQLException {

    }

    @Override
    public void bindGeneratorToJob(String s, TaskGenerator taskGenerator) throws SQLException {
        // TODO bind TaskGenerator to DataArchiveTaskUnit.
    }

    @Override
    public JobStatistic getJobStatistic(String s) throws JobException {
        return new JobStatistic();
    }

    @Override
    public void storeJobStatistic(JobMeta jobMeta) throws JobSqlException {
        jobMeta.getJobStat().buildReportData();
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
    public void updateLimiter(JobMeta jobMeta) {
        RateLimitConfiguration ratelimit;
        try {
            ratelimit = limiterService
                    .getByOrderIdOrElseDefaultConfig(Long.parseLong(DlmJobIdUtil.getJobName(jobMeta.getJobId())));
        } catch (Exception e) {
            log.warn("Update limiter failed,jobId={},error={}",
                    jobMeta.getJobId(), e);
            return;
        }
        setClusterLimitConfig(jobMeta.getSourceCluster(), ratelimit.getDataSizeLimit());
        setClusterLimitConfig(jobMeta.getTargetCluster(), ratelimit.getDataSizeLimit());
        setTenantLimitConfig(jobMeta.getSourceTenant(), ratelimit.getDataSizeLimit());
        setTenantLimitConfig(jobMeta.getTargetTenant(), ratelimit.getDataSizeLimit());
        setTableLimitConfig(jobMeta.getSourceTableMeta(), ratelimit.getRowLimit());
        setTableLimitConfig(jobMeta.getTargetTableMeta(), ratelimit.getRowLimit());
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
