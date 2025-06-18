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

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.loaddata.LoadDataJobBuilder;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobScheduler;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/9/2
 */
public class LoadDataJob implements OdcJob {
    private final LoadDataJobBuilder jobBuilder;
    private final JobScheduler jobScheduler;
    public final ScheduleTaskService scheduleTaskService;
    private Long jobId;

    public LoadDataJob() {
        Verify.verify(SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class).isEnabled(),
                "Load data is supported only when task framework enabled");
        jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        jobBuilder = SpringContextUtil.getBean(LoadDataJobBuilder.class);
        scheduleTaskService = SpringContextUtil.getBean(ScheduleTaskService.class);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        jobId = jobScheduler.scheduleJobNow(jobBuilder.build(jobExecutionContext));
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) jobExecutionContext.getResult();
        scheduleTaskService.updateJobIdByTaskIdWithCheckScheduleTaskCancelingStatus(taskEntity.getId(), jobId);
    }

    @Override
    public void before(JobExecutionContext jobExecutionContext) {

    }

    @Override
    public void after(JobExecutionContext jobExecutionContext) {

    }

    @Override
    public void interrupt() {
        if (jobId != null) {
            try {
                jobScheduler.cancelJob(jobId);
            } catch (JobException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
