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

package com.oceanbase.odc.service.schedule.job;

import java.util.List;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.dlm.JobMetaFactory;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/12/27 17:04
 * @Descripition:
 */
@Slf4j
public class CloudDataArchiveJob extends DataArchiveJob {

    @Override
    public void execute(JobExecutionContext context) {

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();

        List<DlmTask> taskUnits = getTaskUnits(taskEntity);

        JobMetaFactory jobMetaFactory = SpringContextUtil.getBean(JobMetaFactory.class);
        JobDefinition jobDefinition = jobMetaFactory.createJobDefinition(taskUnits);
        JobScheduler jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        try {
            Long jobId = jobScheduler.scheduleJobNow(jobDefinition);
            log.info("Schedule job success,jobId={}", jobId);
        } catch (JobException e) {
            log.warn("Schedule job failed,taskId={}", taskEntity.getId());
            throw new RuntimeException(e);
        }
    }
}
