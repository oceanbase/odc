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
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.oceanbase.odc.service.schedule.model.DlmExecutionDetail;

/**
 * @Author：tinker
 * @Date: 2024/2/23 11:48
 * @Descripition:
 */
@Service
public class DLMService {

    @Autowired
    private DlmTableUnitRepository dlmJobRepository;

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
        return dlmJobRepository.findByScheduleTaskId(scheduleTaskId).stream()
                .map(o -> JsonUtils.fromJson(o.getExecutionDetail(), DlmExecutionDetail.class))
                .collect(Collectors.toList());
    }

    public List<DlmTableUnitEntity> createJob(List<DlmTableUnit> dlmTableUnits) {
        return dlmJobRepository
                .saveAll(dlmTableUnits.stream().map(DlmTableUnitMapper::modelToEntity).collect(Collectors.toList()));
    }

    public void updateDlmJobStatus(String dlmTableUnitId, TaskStatus status) {
        dlmJobRepository.updateStatusByDlmTableUnitId(dlmTableUnitId, status);
    }

    public List<DlmTableUnit> findByScheduleTaskId(Long scheduleTaskId) {
        return dlmJobRepository.findByScheduleTaskId(scheduleTaskId).stream().map(DlmTableUnitMapper::entityToModel)
                .collect(
                        Collectors.toList());
    }

}
