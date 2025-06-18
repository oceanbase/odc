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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;

/**
 * @Author：tinker
 * @Date: 2024/2/23 11:48
 * @Descripition:
 */
@Service
public class DLMService {

    @Autowired
    private DlmTableUnitRepository dlmTableUnitRepository;
    @Autowired
    @Qualifier("supportsRestartTaskTypes")
    private Set<ScheduleTaskType> supportsRestartTaskTypes;

    @SkipAuthorize("do not access any resources")
    public List<String> previewSqlStatements(PreviewSqlStatementsReq req) {
        List<String> returnValue = new LinkedList<>();
        String previewSqlTemp = "select * from %s where %s;";
        Date now = new Date();
        req.getTables().forEach(tableConfig -> {
            StringBuilder sb = new StringBuilder();
            tableConfig.getJoinTableConfigs().forEach(joinTableConfig -> {
                sb.append(String.format(" join %s on %s", joinTableConfig.getTableName(),
                        joinTableConfig.getJoinCondition()));
            });
            returnValue.add(String.format(previewSqlTemp, tableConfig.getTableName() + sb,
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
    public void createOrUpdateDlmTableUnits(List<DlmTableUnit> dlmTableUnits) {
        dlmTableUnits.forEach(o -> {
            Optional<DlmTableUnitEntity> entityOptional = dlmTableUnitRepository.findByDlmTableUnitId(
                    o.getDlmTableUnitId());
            DlmTableUnitEntity entity;
            if (entityOptional.isPresent()) {
                entity = entityOptional.get();
                if (entity.getStatus() == TaskStatus.DONE) {
                    return;
                }
                entity.setStatistic(JsonUtils.toJson(o.getStatistic()));
                entity.setStatus(o.getStatus());
                entity.setStartTime(o.getStartTime());
                entity.setEndTime(o.getEndTime());
            } else {
                entity = DlmTableUnitMapper.modelToEntity(o);
            }
            dlmTableUnitRepository.save(entity);
        });
    }

    @SkipAuthorize("odc internal usage")
    public List<DlmTableUnit> findByScheduleTaskId(Long scheduleTaskId) {
        return dlmTableUnitRepository.findByScheduleTaskId(scheduleTaskId).stream()
                .map(DlmTableUnitMapper::entityToModel)
                .collect(
                        Collectors.toList());
    }

    /**
     * generate final task status by scheduleTaskId when the task is finished
     */
    @SkipAuthorize("odc internal usage")
    public TaskStatus getFinalTaskStatus(ScheduleTask scheduleTask, ScheduleTaskType scheduleTaskType) {
        // if the task is pausing, return PAUSED as final status
        if (scheduleTask.getStatus() == TaskStatus.PAUSING) {
            return TaskStatus.PAUSED;
        }
        // if the task is canceling, return CANCELED as final status
        if (scheduleTask.getStatus() == TaskStatus.CANCELING) {
            return TaskStatus.CANCELED;
        }
        List<DlmTableUnit> dlmTableUnits = findByScheduleTaskId(scheduleTask.getId());
        Set<TaskStatus> collect = dlmTableUnits.stream().map(DlmTableUnit::getStatus).collect(
                Collectors.toSet());
        boolean enableRestart = supportsRestartTaskTypes.contains(scheduleTaskType);
        // If any table is timeout, the task is considered timeout.
        if (collect.contains(TaskStatus.EXEC_TIMEOUT)) {
            return TaskStatus.EXEC_TIMEOUT;
        }
        // If the tables do not exist or any table fails, the task is considered a failure.
        if (dlmTableUnits.isEmpty() || collect.contains(TaskStatus.FAILED)) {
            // if enableRestart, return ABNORMAL instead of FAILED to let the task be restarted.
            return enableRestart ? TaskStatus.ABNORMAL : TaskStatus.FAILED;
        }
        // If any table is canceled, the task is considered canceled.
        if (collect.contains(TaskStatus.CANCELED)) {
            return TaskStatus.CANCELED;
        }
        // The task is considered failed if any table is still preparing or running when the task is
        // finished.
        if (collect.contains(TaskStatus.PREPARING) || collect.contains(TaskStatus.RUNNING)) {
            // if enableRestart, return ABNORMAL instead of FAILED to let the task be restarted.
            return enableRestart ? TaskStatus.ABNORMAL : TaskStatus.FAILED;
        }
        return TaskStatus.DONE;
    }

}
