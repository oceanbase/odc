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

import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.CLUSTER_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.CONNECTION_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.CREATOR_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.CREATOR_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.DATABASE_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.DATABASE_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.ENVIRONMENT;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.PROJECT_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.PROJECT_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_ID;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_STATUS;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TASK_TYPE;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TENANT_NAME;
import static com.oceanbase.odc.service.notification.constant.EventLabelKeys.TRIGGER_TIME;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.TaskEvent;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/18
 */
@Component
public class EventBuilder {
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private UserService userService;

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

    private Event ofTask(TaskEntity task, TaskEvent status) {
        EventLabels labels = new EventLabels();
        labels.putIfNonNull(TASK_TYPE, task.getTaskType().name());
        labels.putIfNonNull(TASK_STATUS, status.name());
        labels.putIfNonNull(TASK_ID, task.getId() + "");
        labels.putIfNonNull(CONNECTION_ID, task.getConnectionId());
        labels.putIfNonNull(CREATOR_ID, task.getCreatorId());
        labels.putIfNonNull(TRIGGER_TIME, new Date(System.currentTimeMillis()));

        Verify.notNull(task.getDatabaseId(), "database id");
        Database database = databaseService.detailSkipPermissionCheck(task.getDatabaseId());
        labels.putIfNonNull(DATABASE_ID, database.id());
        labels.putIfNonNull(DATABASE_NAME, database.getName());
        labels.putIfNonNull(PROJECT_ID, database.getProject().id());
        labels.putIfNonNull(PROJECT_NAME, database.getProject().getName());

        return Event.builder()
                .status(EventStatus.CREATED)
                .creatorId(task.getCreatorId())
                .organizationId(task.getOrganizationId())
                .projectId(database.getProject().id())
                .triggerTime(new Date(System.currentTimeMillis()))
                .labels(labels)
                .build();
    }

    private void resolveLabels(EventLabels labels) {
        Verify.notNull(labels, "event.labels");

        if (labels.containsKey(CONNECTION_ID)) {
            ConnectionConfig connectionConfig = connectionService.getForConnectionSkipPermissionCheck(
                    Long.parseLong(labels.get(CONNECTION_ID)));
            labels.put(CLUSTER_NAME, connectionConfig.getClusterName());
            labels.put(TENANT_NAME, connectionConfig.getTenantName());
            Environment environment = environmentService.detail(connectionConfig.getEnvironmentId());
            labels.put(ENVIRONMENT, environment.getName());
        }
        if (labels.containsKey(CREATOR_ID)) {
            UserEntity user = userService.nullSafeGet(Long.parseLong(labels.get(CREATOR_ID)));
            labels.putIfNonNull(CREATOR_NAME, user.getName());
        }
    }

}
