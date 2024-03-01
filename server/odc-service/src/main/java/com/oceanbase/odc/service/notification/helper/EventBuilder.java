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
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.REGION;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_ENTITY_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_STATUS;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_TYPE;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TENANT_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TRIGGER_TIME;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
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
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.TaskEvent;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter.ApplyDatabase;
import com.oceanbase.odc.service.permission.project.ApplyProjectParameter;
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
    private static final String OB_ARN_PARTITION = System.getenv("OB_ARN_PARTITION");
    private static final String AUTO_APPROVAL_KEY =
            "${com.oceanbase.odc.builtin-resource.regulation.approval.flow.config.auto-approval.name}";

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
    private ScheduleRepository scheduleRepository;
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

    public Event ofPendingApprovalTask(TaskEntity task, Collection<Long> approvers) {
        Event event = ofTask(task, TaskEvent.PENDING_APPROVAL);
        event.getLabels().putIfNonNull(APPROVER_ID, JsonUtils.toJson(approvers));
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

    public Event ofSucceededTask(ScheduleEntity schedule) {
        Event event = ofSchedule(schedule, TaskEvent.EXECUTION_SUCCEEDED);
        resolveLabels(event.getLabels());
        return event;
    }

    public Event ofFailedTask(ScheduleEntity schedule) {
        Event event = ofSchedule(schedule, TaskEvent.EXECUTION_FAILED);
        resolveLabels(event.getLabels());
        return event;
    }

    public Event ofFailedSchedule(ScheduleEntity schedule) {
        Event event = ofSchedule(schedule, TaskEvent.SCHEDULING_FAILED);
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
        labels.putIfNonNull(REGION, OB_ARN_PARTITION);

        Long projectId;
        if (Objects.nonNull(task.getDatabaseId())) {
            Database database = databaseService.getBasicSkipPermissionCheck(task.getDatabaseId());
            labels.putIfNonNull(DATABASE_ID, database.id());
            labels.putIfNonNull(DATABASE_NAME, database.getName());
            labels.putIfNonNull(PROJECT_ID, database.getProject().id());
            projectId = database.getProject().id();
        } else if (task.getTaskType() == TaskType.APPLY_DATABASE_PERMISSION) {
            ApplyDatabaseParameter parameter =
                    JsonUtils.fromJson(task.getParametersJson(), ApplyDatabaseParameter.class);
            List<String> dbNames =
                    parameter.getDatabases().stream().map(ApplyDatabase::getName).collect(Collectors.toList());
            labels.putIfNonNull(DATABASE_NAME, dbNames);
            projectId = parameter.getProject().getId();
            labels.putIfNonNull(PROJECT_ID, projectId);
        } else if (task.getTaskType() == TaskType.APPLY_PROJECT_PERMISSION) {
            ApplyProjectParameter parameter =
                    JsonUtils.fromJson(task.getParametersJson(), ApplyProjectParameter.class);
            projectId = parameter.getProject().getId();
            labels.putIfNonNull(PROJECT_ID, projectId);
        } else {
            throw new UnexpectedException("task.databaseId should not be null");
        }

        return Event.builder()
                .status(EventStatus.CREATED)
                .creatorId(task.getCreatorId())
                .organizationId(task.getOrganizationId())
                .projectId(projectId)
                .triggerTime(new Date())
                .labels(labels)
                .build();
    }

    private Event ofSchedule(ScheduleEntity schedule, TaskEvent status) {
        EventLabels labels = new EventLabels();
        labels.putIfNonNull(TASK_STATUS, status.name());
        labels.putIfNonNull(TRIGGER_TIME, LocalDateTime.now().format(DATE_FORMATTER));
        labels.putIfNonNull(REGION, OB_ARN_PARTITION);

        switch (schedule.getJobType()) {
            case DATA_ARCHIVE:
            case DATA_ARCHIVE_DELETE:
            case DATA_ARCHIVE_ROLLBACK:
                labels.putIfNonNull(TASK_TYPE, JobType.DATA_ARCHIVE);
                break;
            default:
                labels.putIfNonNull(TASK_TYPE, schedule.getJobType());
                break;
        }
        labels.putIfNonNull(TASK_ID, schedule.getId());
        labels.putIfNonNull(CONNECTION_ID, schedule.getConnectionId());
        labels.putIfNonNull(CREATOR_ID, schedule.getCreatorId());
        labels.putIfNonNull(PROJECT_ID, schedule.getProjectId());
        labels.putIfNonNull(DATABASE_ID, schedule.getDatabaseId());
        labels.putIfNonNull(DATABASE_NAME, schedule.getDatabaseName());

        return Event.builder()
                .status(EventStatus.CREATED)
                .creatorId(schedule.getCreatorId())
                .organizationId(schedule.getOrganizationId())
                .projectId(schedule.getProjectId())
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
                Environment environment =
                        environmentService.detailSkipPermissionCheck(connectionConfig.getEnvironmentId());
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
                if ("null".equals(labels.get(APPROVER_ID))) {
                    labels.putIfNonNull(APPROVER_NAME, AUTO_APPROVAL_KEY);
                } else if (labels.get(APPROVER_ID).startsWith("[")) {
                    List<Long> approverIds = JsonUtils.fromJsonList(labels.get(APPROVER_ID), Long.class);
                    List<User> approvers = userService.batchNullSafeGet(approverIds);
                    labels.putIfNonNull(APPROVER_NAME,
                            String.join(" | ", approvers.stream().map(User::getName).collect(Collectors.toSet())));
                } else {
                    UserEntity user = userService.nullSafeGet(labels.getLongFromString(APPROVER_ID));
                    labels.putIfNonNull(APPROVER_NAME, user.getName());
                }
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
                if (Objects.nonNull(parentInstanceId)) {
                    if ("ASYNC".equals(labels.get(TASK_TYPE))) {
                        Optional<ScheduleEntity> optional = scheduleRepository.findById(parentInstanceId);
                        if (optional.isPresent() && optional.get().getJobType() == JobType.SQL_PLAN) {
                            labels.putIfNonNull(TASK_TYPE, JobType.SQL_PLAN);
                            labels.putIfNonNull(TASK_ID, parentInstanceId);
                        } else {
                            labels.putIfNonNull(TASK_ID, flowInstances.get(0).getId());
                        }
                    } else if ("ALTER_SCHEDULE".equals(labels.get(TASK_TYPE))) {
                        scheduleRepository.findById(parentInstanceId).ifPresent(schedule -> {
                            labels.putIfNonNull(TASK_TYPE, schedule.getJobType());
                            labels.putIfNonNull(TASK_ID, parentInstanceId);
                        });
                    } else {
                        labels.putIfNonNull(TASK_ID, flowInstances.get(0).getId());
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
                Project project = projectService.getBasicSkipPermissionCheck(labels.getLongFromString(PROJECT_ID));
                labels.putIfNonNull(PROJECT_NAME, project.getName());
            } catch (Exception e) {
                log.warn("failed to query project info.", e);
            }
        }
    }

}
