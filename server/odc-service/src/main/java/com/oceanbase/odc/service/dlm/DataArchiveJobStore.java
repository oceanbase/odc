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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.dlm.TaskGeneratorEntity;
import com.oceanbase.odc.metadb.dlm.TaskGeneratorRepository;
import com.oceanbase.odc.metadb.dlm.TaskUnitEntity;
import com.oceanbase.odc.metadb.dlm.TaskUnitRepository;
import com.oceanbase.odc.service.dlm.model.DlmLimiterConfig;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.dlm.utils.TaskGeneratorMapper;
import com.oceanbase.odc.service.dlm.utils.TaskUnitMapper;
import com.oceanbase.tools.migrator.common.dto.JobStatistic;
import com.oceanbase.tools.migrator.common.dto.TableSizeInfo;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;
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

    @Value("${odc.task.dlm.support-resume:false}")
    private boolean supportResume;
    @Autowired
    private DlmLimiterService limiterService;
    @Autowired
    private TaskGeneratorRepository taskGeneratorRepository;
    @Autowired
    private TaskUnitRepository taskUnitRepository;

    private final TaskGeneratorMapper taskGeneratorMapper = TaskGeneratorMapper.INSTANCE;
    private final TaskUnitMapper taskUnitMapper = TaskUnitMapper.INSTANCE;

    @Override
    public TaskGenerator getTaskGenerator(String generatorId, String jobId) {
        if (supportResume) {
            return taskGeneratorRepository.findByGeneratorId(jobId).map(taskGeneratorMapper::entityToModel)
                    .orElse(null);
        }
        return null;
    }

    @Override
    public void storeTaskGenerator(TaskGenerator taskGenerator) {
        if (supportResume) {
            Optional<TaskGeneratorEntity> optional = taskGeneratorRepository.findByGeneratorId(taskGenerator.getId());
            TaskGeneratorEntity entity;
            if (optional.isPresent()) {
                entity = optional.get();
                entity.setStatus(taskGenerator.getGeneratorStatus().name());
                entity.setTaskCount(taskGenerator.getTaskCount());
                entity.setPartitionSavePoint(taskGenerator.getGeneratorPartitionSavepoint());
                entity.setProcessedRowCount(taskGenerator.getProcessedRowCount());
                entity.setProcessedDataSize(taskGenerator.getProcessedDataSize());
                if (taskGenerator.getGeneratorSavePoint() != null) {
                    entity.setPrimaryKeySavePoint(taskGenerator.getGeneratorSavePoint().toSqlString());
                }
            } else {
                entity = taskGeneratorMapper.modelToEntity(taskGenerator);
            }
            taskGeneratorRepository.save(entity);
        }
    }

    @Override
    public void bindGeneratorToJob(String jobId, TaskGenerator taskGenerator) {}

    @Override
    public JobStatistic getJobStatistic(String s) {
        return new JobStatistic();
    }

    @Override
    public void storeJobStatistic(JobMeta jobMeta) {
        jobMeta.getJobStat().buildReportData();
    }

    @Override
    public List<TaskMeta> getTaskMeta(JobMeta jobMeta) {
        if (supportResume) {
            List<TaskMeta> tasks = taskUnitRepository.findByGeneratorId(jobMeta.getGenerator().getId()).stream().map(
                    taskUnitMapper::entityToModel).collect(
                            Collectors.toList());
            tasks.forEach(o -> o.setJobMeta(jobMeta));
            return tasks;
        }
        return null;
    }

    @Override
    public void storeTaskMeta(TaskMeta taskMeta) {
        if (supportResume) {
            Optional<TaskUnitEntity> optional = taskUnitRepository.findByJobIdAndGeneratorIdAndTaskIndex(
                    taskMeta.getJobMeta().getJobId(), taskMeta.getGeneratorId(), taskMeta.getTaskIndex());
            TaskUnitEntity entity;
            if (optional.isPresent()) {
                entity = optional.get();
                entity.setStatus(taskMeta.getTaskStatus().name());
                entity.setPartitionName(taskMeta.getPartitionName());
                if (taskMeta.getMinPrimaryKey() != null) {
                    entity.setLowerBoundPrimaryKey(taskMeta.getMinPrimaryKey().toSqlString());
                }
                if (taskMeta.getMaxPrimaryKey() != null) {
                    entity.setUpperBoundPrimaryKey(taskMeta.getMaxPrimaryKey().toSqlString());
                }
                if (taskMeta.getCursorPrimaryKey() != null) {
                    entity.setPrimaryKeyCursor(taskMeta.getCursorPrimaryKey().toSqlString());
                }
            } else {
                entity = taskUnitMapper.modelToEntity(taskMeta);
            }
            taskUnitRepository.save(entity);
        }
    }

    @Override
    public Long getAbnormalTaskIndex(String jobId) {
        if (supportResume) {
            Long abnormalTaskCount = taskUnitRepository.findAbnormalTaskByJobId(jobId);
            if (abnormalTaskCount != 0) {
                return abnormalTaskCount;
            }
        }
        return null;
    }

    @Override
    public void updateTableSizeInfo(TableSizeInfo tableSizeInfo, long l) {

    }

    @Override
    public void updateLimiter(JobMeta jobMeta) {
        DlmLimiterConfig limiterConfig;
        try {
            limiterConfig = limiterService
                    .getByOrderIdOrElseDefaultConfig(Long.parseLong(DlmJobIdUtil.getJobName(jobMeta.getJobId())));
        } catch (Exception e) {
            log.warn("Update limiter failed,jobId={},error={}",
                    jobMeta.getJobId(), e);
            return;
        }
        setClusterLimitConfig(jobMeta.getSourceCluster(), limiterConfig.getDataSizeLimit());
        setClusterLimitConfig(jobMeta.getTargetCluster(), limiterConfig.getDataSizeLimit());
        setTenantLimitConfig(jobMeta.getSourceTenant(), limiterConfig.getDataSizeLimit());
        setTenantLimitConfig(jobMeta.getTargetTenant(), limiterConfig.getDataSizeLimit());
        setTableLimitConfig(jobMeta.getSourceTableMeta(), limiterConfig.getRowLimit());
        setTableLimitConfig(jobMeta.getTargetTableMeta(), limiterConfig.getRowLimit());
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
