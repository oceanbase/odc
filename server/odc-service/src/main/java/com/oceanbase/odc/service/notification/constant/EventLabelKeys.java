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
package com.oceanbase.odc.service.notification.constant;

/**
 * @Author: Lebie
 * @Date: 2023/3/23 14:47
 * @Description: []
 */
public class EventLabelKeys {
    /**
     * basic info
     */
    public static final String TASK_ID = "taskId";
    // internal usage
    public static final String TASK_ENTITY_ID = "taskEntityId";
    public static final String TASK_TYPE = "taskType";
    public static final String TASK_STATUS = "taskStatus";
    public static final String TRIGGER_TIME = "triggerTime";

    /**
     * connection info
     */
    public static final String CONNECTION_ID = "connectionId";
    public static final String ENVIRONMENT = "environment";
    public static final String CLUSTER_NAME = "clusterName";
    public static final String TENANT_NAME = "tenantName";
    public static final String DATABASE_ID = "databaseId";
    public static final String DATABASE_NAME = "databaseName";
    public static final String TABLE_NAME = "tableName";

    /**
     * collaboration info
     */
    public static final String PROJECT_ID = "projectId";
    public static final String PROJECT_NAME = "projectName";
    public static final String CREATOR_ID = "creatorId";
    public static final String CREATOR_NAME = "creatorName";
    public static final String APPROVER_ID = "approverId";
    public static final String APPROVER_NAME = "approverName";

    /**
     * global info
     */
    public static final String REGION = "region";
    public static final String TICKET_URL = "ticketUrl";

}
