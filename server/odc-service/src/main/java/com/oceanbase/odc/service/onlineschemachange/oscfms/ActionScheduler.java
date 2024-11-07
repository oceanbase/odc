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
package com.oceanbase.odc.service.onlineschemachange.oscfms;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.onlineschemachange.OnlineSchemaChangeContextHolder;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlConstants;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * time based scheduler for action
 * 
 * @author longpeng.zlp
 * @date 2024/7/11 09:58
 * @since 4.3.1
 */
@Slf4j
@Component
public class ActionScheduler {
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private QuartzJobService quartzJobService;

    public void submitFMSScheduler(ScheduleEntity scheduleEntity, Long scheduleTaskId, Long taskID) {
        Long scheduleId = scheduleEntity.getId();
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, JobType.ONLINE_SCHEMA_CHANGE_COMPLETE);
        Map<String, Object> triggerData = getStringObjectMap(scheduleTaskId, scheduleEntity, taskID);
        try {
            if (quartzJobService.checkExists(jobKey)) {
                scheduleService.innerUpdateTriggerData(scheduleId, triggerData);
                return;
            }
        } catch (SchedulerException e) {
            throw new IllegalArgumentException(e);
        }

        try {
            scheduleService.innerEnable(scheduleId, triggerData);
            log.info("Start online schema change by quartz job, jobParameters={}",
                    JsonUtils.toJson(scheduleEntity.getJobParametersJson()));
        } catch (SchedulerException e) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Create a quartz job check oms project occur error, jobParameters={0}",
                    JsonUtils.toJson(scheduleEntity.getJobParametersJson())), e);
        }
    }

    public void cancelScheduler(Long scheduleId) {
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, JobType.ONLINE_SCHEMA_CHANGE_COMPLETE);
        try {
            quartzJobService.deleteJob(jobKey);
            log.info("Successfully delete job with jobKey={}", jobKey);
        } catch (SchedulerException e) {
            log.warn("Delete job occur error with jobKey={}", jobKey, e);
        }
    }

    private static Map<String, Object> getStringObjectMap(Long scheduleTaskId, ScheduleEntity scheduleEntity,
            Long taskID) {
        Map<String, Object> dataMap = new HashMap<>(2);
        dataMap.put(OdcConstants.SCHEDULE_TASK_ID, scheduleTaskId);
        Map<String, String> mdcContext = new HashMap<>();
        mdcContext.put(OnlineSchemaChangeContextHolder.TASK_WORK_SPACE, String.valueOf(scheduleEntity.getCreatorId()));
        mdcContext.put(OdcConstants.ORGANIZATION_ID, String.valueOf(scheduleEntity.getOrganizationId()));
        mdcContext.put(OnlineSchemaChangeContextHolder.TASK_ID, String.valueOf(taskID));
        dataMap.put(DdlConstants.MDC_CONTEXT, JsonUtils.toJson(mdcContext));
        return dataMap;
    }
}
