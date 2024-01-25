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
package com.oceanbase.odc.service.notification.helper;

import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.APPROVER_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.APPROVER_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.CLUSTER_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.CONNECTION_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.CREATOR_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.CREATOR_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.DATABASE_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.DATABASE_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.ENVIRONMENT;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.PROJECT_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.PROJECT_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_ENTITY_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_STATUS;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_TYPE;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TENANT_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TRIGGER_TIME;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.TaskEvent;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/18
 */
@Component
@Slf4j
public class EventBuilder {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private UserService userService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ProjectService projectService;

    public Event ofFailedTask(TaskEntity task) {
        Event event = ofTask(task, TaskEvent.EXECUTION_FAILED);
        resolveLabels(event.getLabels());
        return event;
    }

    public Event ofSucceededTask(TaskEntity task) {
        Event event = ofTask(task, TaskEvent.EXECUTION_SUCCEEDED);
        resolveLabels(event.getLabels());
        return event;
    }

    public Event ofTimeoutTask(TaskEntity task) {
        Event event = ofTask(task, TaskEvent.EXECUTION_TIMEOUT);
        resolveLabels(event.getLabels());
        return event;
    }

    public Event ofPendingApprovalTask(TaskEntity task) {
        Event event = ofTask(task, TaskEvent.PENDING_APPROVAL);
        resolveLabels(event.getLabels());
        return event;
    }

    public Event ofApprovedTask(TaskEntity task, Long approver) {
        Event event = ofTask(task, TaskEvent.APPROVED);
        event.getLabels().put(APPROVER_ID, approver + "");
        resolveLabels(event.getLabels());
        return event;
    }

    public Event ofRejectedTask(TaskEntity task, Long approver) {
        Event event = ofTask(task, TaskEvent.APPROVAL_REJECTION);
        event.getLabels().put(APPROVER_ID, approver + "");
        resolveLabels(event.getLabels());
        return event;
    }

    private Event ofTask(TaskEntity task, TaskEvent status) {
        EventLabels labels = new EventLabels();
        labels.putIfNonNull(TASK_TYPE, task.getTaskType().name());
        labels.putIfNonNull(TASK_STATUS, status.name());
        labels.putIfNonNull(TASK_ENTITY_ID, task.getId());
        labels.putIfNonNull(CONNECTION_ID, task.getConnectionId());
        labels.putIfNonNull(CREATOR_ID, task.getCreatorId());
        labels.putIfNonNull(TRIGGER_TIME, LocalDateTime.now().format(DATE_FORMATTER));

        Verify.notNull(task.getDatabaseId(), "database id");
        Database database = databaseService.detailSkipPermissionCheck(task.getDatabaseId());
        labels.putIfNonNull(DATABASE_ID, database.id());
        labels.putIfNonNull(DATABASE_NAME, database.getName());
        labels.putIfNonNull(PROJECT_ID, database.getProject().id());

        return Event.builder()
                .status(EventStatus.CREATED)
                .creatorId(task.getCreatorId())
                .organizationId(task.getOrganizationId())
                .projectId(database.getProject().id())
                .triggerTime(new Date())
                .labels(labels)
                .build();
    }

    private void resolveLabels(EventLabels labels) {
        Verify.notNull(labels, "event.labels");

        if (labels.containsKey(CONNECTION_ID)) {
            try {
                ConnectionConfig connectionConfig = connectionService.getForConnectionSkipPermissionCheck(
                        labels.getLongFromString(CONNECTION_ID));
                labels.put(CLUSTER_NAME, connectionConfig.getClusterName());
                labels.put(TENANT_NAME, connectionConfig.getTenantName());
                Environment environment = environmentService.detail(connectionConfig.getEnvironmentId());
                labels.put(ENVIRONMENT, environment.getName());
            } catch (Exception e) {
                log.warn("failed to query connection info.", e);
            }
        }
        if (labels.containsKey(CREATOR_ID)) {
            try {
                UserEntity user = userService.nullSafeGet(labels.getLongFromString(CREATOR_ID));
                labels.putIfNonNull(CREATOR_NAME, user.getName());
            } catch (Exception e) {
                log.warn("failed to query creator info.", e);
            }
        }
        if (labels.containsKey(APPROVER_ID)) {
            try {
                UserEntity user = userService.nullSafeGet(labels.getLongFromString(APPROVER_ID));
                labels.putIfNonNull(APPROVER_NAME, user.getName());
            } catch (Exception e) {
                log.warn("failed to query approver.", e);
            }
        }
        if (labels.containsKey(TASK_ENTITY_ID)) {
            try {
                List<FlowInstanceEntity> flowInstances =
                        flowInstanceRepository.findByTaskId(labels.getLongFromString(TASK_ENTITY_ID));
                Verify.singleton(flowInstances, "flow instance");
                Long parentInstanceId = flowInstances.get(0).getParentInstanceId();
                if (Objects.nonNull(parentInstanceId)
                        && "ASYNC".equals(labels.get(TASK_TYPE))) {
                    // maybe sql plan or rollback
                    ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(parentInstanceId);
                    if (scheduleEntity.getJobType() == JobType.SQL_PLAN) {
                        labels.putIfNonNull(TASK_TYPE, JobType.SQL_PLAN);
                    }
                } else {
                    labels.putIfNonNull(TASK_ID, flowInstances.get(0).getId());
                }
            } catch (Exception e) {
                log.warn("failed to query task info.", e);
            }
        }
        if (labels.containsKey(PROJECT_ID)) {
            try {
                Project project = projectService.detailSkipPermissionCheck(labels.getLongFromString(PROJECT_ID));
                labels.putIfNonNull(PROJECT_NAME, project.getName());
            } catch (Exception e) {
                log.warn("failed to query project info.", e);
            }
        }
    }

}
