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
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.dlm.DlmTableUnitEntity;
import com.oceanbase.odc.metadb.dlm.DlmTableUnitRepository;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.PreviewSqlStatementsReq;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.dlm.utils.DlmTableUnitMapper;
import com.oceanbase.odc.service.schedule.model.DlmTableUnitExecutionDetail;

/**
 * @Author：tinker
 * @Date: 2024/2/23 11:48
 * @Descripition:
 */
@Service
public class DLMService {

    @Autowired
    private DlmTableUnitRepository dlmTableUnitRepository;

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

    @SkipAuthorize("odc internal usage")
    public String getExecutionDetailByScheduleTaskId(Long scheduleTaskId) {
        List<DlmTableUnitExecutionDetail> details = dlmTableUnitRepository.findByScheduleTaskId(scheduleTaskId).stream()
                .map(entity -> {
                    DlmTableUnit dlmTableUnit = DlmTableUnitMapper.entityToModel(entity);
                    DlmTableUnitExecutionDetail executionDetail = new DlmTableUnitExecutionDetail();
                    executionDetail.setStartTime(dlmTableUnit.getStartTime());
                    executionDetail.setEndTime(dlmTableUnit.getEndTime());
                    executionDetail.setType(dlmTableUnit.getType());
                    executionDetail.setStatus(dlmTableUnit.getStatus());
                    executionDetail.setTableName(dlmTableUnit.getTableName());
                    executionDetail.setProcessedRowCount(dlmTableUnit.getStatistic().getProcessedRowCount());
                    executionDetail.setProcessedRowsPerSecond(dlmTableUnit.getStatistic().getProcessedRowsPerSecond());
                    executionDetail.setReadRowCount(dlmTableUnit.getStatistic().getReadRowCount());
                    executionDetail.setReadRowsPerSecond(dlmTableUnit.getStatistic().getReadRowsPerSecond());
                    executionDetail.setUserCondition(dlmTableUnit.getParameters().getMigrateRule());
                    return executionDetail;
                }).collect(Collectors.toList());
        return JsonUtils.toJson(details);
    }

    @SkipAuthorize("odc internal usage")
    public List<DlmTableUnitEntity> createDlmTableUnits(List<DlmTableUnit> dlmTableUnits) {
        return dlmTableUnitRepository
                .saveAll(dlmTableUnits.stream().map(DlmTableUnitMapper::modelToEntity).collect(Collectors.toList()));
    }

    @SkipAuthorize("odc internal usage")
    @Transactional(rollbackFor = Exception.class)
    public void updateStatusByDlmTableUnitId(String dlmTableUnitId, TaskStatus status) {
        dlmTableUnitRepository.findByDlmTableUnitId(dlmTableUnitId).ifPresent(e -> {
            e.setStatus(status);
            if (status == TaskStatus.RUNNING && e.getStartTime() == null) {
                e.setStartTime(new Date());
            } else if (status.isTerminated()) {
                e.setEndTime(new Date());
            }
            dlmTableUnitRepository.save(e);
        });
    }

    @SkipAuthorize("odc internal usage")
    public List<DlmTableUnit> findByScheduleTaskId(Long scheduleTaskId) {
        return dlmTableUnitRepository.findByScheduleTaskId(scheduleTaskId).stream()
                .map(DlmTableUnitMapper::entityToModel)
                .collect(
                        Collectors.toList());
    }

    public TaskStatus getTaskStatus(Long scheduleTaskId) {
        Set<TaskStatus> collect = findByScheduleTaskId(scheduleTaskId).stream().map(DlmTableUnit::getStatus).collect(
                Collectors.toSet());
        if (collect.contains(TaskStatus.FAILED)) {
            return TaskStatus.FAILED;
        }
        if (collect.contains(TaskStatus.DONE) && collect.size() == 1) {
            return TaskStatus.DONE;
        }
        return TaskStatus.CANCELED;
    }

}
