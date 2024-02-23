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
package com.oceanbase.odc.service.task.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.notification.Broker;
import com.oceanbase.odc.service.notification.NotificationProperties;
import com.oceanbase.odc.service.notification.helper.EventBuilder;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/23
 */
@Component("jobTerminateNotifyListener")
public class JobTerminateNotifyListener extends AbstractEventListener<JobTerminateEvent> {

    @Autowired
    private TaskFrameworkService taskFrameworkService;
    @Autowired
    private NotificationProperties notificationProperties;
    @Autowired
    private Broker broker;
    @Autowired
    private EventBuilder eventBuilder;
    @Autowired
    private ScheduleTaskService scheduleTaskService;

    @Override
    public void onEvent(JobTerminateEvent event) {
        if (!notificationProperties.isEnabled()) {
            return;
        }
        JobEntity jobEntity = taskFrameworkService.find(event.getJi().getId());
        String jobType = jobEntity.getJobType();
        if (!JobType.DATA_DELETE.name().equals(jobType) && !jobType.startsWith(JobType.DATA_ARCHIVE.name())) {
            return;
        }
        // TODO: enqueue event
    }

}
