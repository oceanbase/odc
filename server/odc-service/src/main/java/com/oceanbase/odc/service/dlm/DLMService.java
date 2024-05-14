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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.dlm.DlmJobEntity;
import com.oceanbase.odc.metadb.dlm.DlmJobRepository;
import com.oceanbase.odc.metadb.dlm.DlmJobStatisticEntity;
import com.oceanbase.odc.metadb.dlm.DlmJobStatisticRepository;
import com.oceanbase.odc.service.dlm.model.DlmJob;
import com.oceanbase.odc.service.dlm.model.PreviewSqlStatementsReq;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.dlm.utils.DlmJobMapper;
import com.oceanbase.odc.service.schedule.model.DlmExecutionDetail;
import com.oceanbase.tools.migrator.common.dto.JobParameter;

/**
 * @Author：tinker
 * @Date: 2024/2/23 11:48
 * @Descripition:
 */
@Service
public class DLMService {

    @Autowired
    private DlmJobStatisticRepository dlmJobStatisticRepository;
    @Autowired
    private DlmJobRepository dlmJobRepository;

    @SkipAuthorize("do not access any resources")
    public List<String> previewSqlStatements(PreviewSqlStatementsReq req) {
        List<String> returnValue = new LinkedList<>();
        String previewSqlTemp = "select * from %s where %s;";
        Date now = new Date();
        req.getTables().forEach(tableConfig -> {
            returnValue.add(String.format(previewSqlTemp, tableConfig.getTableName(),
                    StringUtils.isEmpty(tableConfig.getConditionExpression()) ? "1=1"
                            : DataArchiveConditionUtil
                                    .parseCondition(tableConfig.getConditionExpression(), req.getVariables(), now)));
        });
        return returnValue;
    }

    public List<DlmExecutionDetail> getExecutionDetailByScheduleTaskId(Long scheduleTaskId) {

        List<DlmExecutionDetail> details = dlmJobRepository.findByScheduleTaskId(scheduleTaskId).stream().map(o -> {
            DlmExecutionDetail detail = new DlmExecutionDetail();
            detail.setDlmJobId(o.getDlmJobId());
            JobParameter jobParameter = JsonUtils.fromJson(o.getParameters(), JobParameter.class);
            detail.setUserCondition(jobParameter.getMigrateRule());
            detail.setTableName(o.getTableName());
            return detail;
        }).collect(Collectors.toList());
        Map<String, DlmJobStatisticEntity> jobId2JobStatistic =
                listJobStatisticByJobId(details.stream().map(DlmExecutionDetail::getDlmJobId).collect(
                        Collectors.toList()))
                                .stream().collect(Collectors.toMap(DlmJobStatisticEntity::getDlmJobId, o -> o));
        details.forEach(detail -> {
            DlmJobStatisticEntity jobStatistic = jobId2JobStatistic.get(detail.getDlmJobId());
            detail.setReadRowCount(jobStatistic.getReadRowCount());
            detail.setProcessedRowCount(jobStatistic.getProcessedRowCount());
            detail.setReadRowsPerSecond(jobStatistic.getReadRowsPerSecond());
            detail.setProcessedRowsPerSecond(jobStatistic.getProcessedRowsPerSecond());
        });
        return details;
    }

    public List<DlmJobEntity> createJob(List<DlmJob> jobs) {
        List<DlmJobEntity> jobEntities = jobs.stream().map(DlmJobMapper::modelToEntity).collect(Collectors.toList());
        return dlmJobRepository.saveAll(jobEntities);
    }

    public void updateDlmJobStatus(String dlmJobId, TaskStatus status) {
        dlmJobRepository.updateStatusByDlmJobId(dlmJobId, status);
    }

    public List<DlmJob> findByScheduleTaskId(Long scheduleTaskId) {
        return dlmJobRepository.findByScheduleTaskId(scheduleTaskId).stream().map(DlmJobMapper::entityToModel).collect(
                Collectors.toList());
    }

    public List<DlmJobStatisticEntity> listJobStatisticByJobId(List<String> jobId) {
        return dlmJobStatisticRepository.findByDlmJobIdIn(jobId);
    }
}
