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
package com.oceanbase.odc.service.task.processor.terminate;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseService;
import com.oceanbase.odc.service.schedule.model.PublishLogicalDatabaseChangeReq;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.processor.matcher.LogicalDBChangeProcessorMatcher;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/10/10 13:59
 */
@Component
@Slf4j
public class LogicalDBChangeTerminateProcessor extends LogicalDBChangeProcessorMatcher implements TerminateProcessor {
    @Autowired
    protected LogicalDatabaseService logicalDatabaseService;

    @Override
    public void process(ScheduleTask scheduleTask, JobEntity jobEntity) {
        try {
            PublishLogicalDatabaseChangeReq req = JsonUtils.fromJson(JsonUtils
                    .fromJson(jobEntity.getJobParametersJson(),
                            new TypeReference<Map<String, String>>() {})
                    .get(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY),
                    PublishLogicalDatabaseChangeReq.class);
            if (req != null && req.getLogicalDatabaseResp() != null) {
                logicalDatabaseService.extractLogicalTablesSkipAuth(req.getLogicalDatabaseResp().getId(),
                        req.getCreatorId());
                log.info("Submit the extract logical tables task succeed, logicalDatabaseId={}, jobId={}",
                        req.getLogicalDatabaseResp().getId(), jobEntity.getId());
            }
        } catch (Exception ex) {
            log.warn("Failed to submit the extract logical tables task, ex=", ex);
        }
    }
}
