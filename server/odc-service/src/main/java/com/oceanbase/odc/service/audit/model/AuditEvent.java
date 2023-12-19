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
package com.oceanbase.odc.service.audit.model;

import java.util.Date;

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/1/18 下午7:47
 * @Description: []
 */
@Data
@Builder
public class AuditEvent {
    /**
     * ID of the audit event
     */
    private Long id;

    /**
     * Audit event type
     */
    private AuditEventType type;

    /**
     * Audit event name
     */
    private String typeName;

    /**
     * Audit event action
     */
    private AuditEventAction action;

    /**
     * Audit event action name
     */
    private String actionName;

    /**
     * Database id for this event
     */
    private Long databaseId;

    /**
     * Database name for this event; Null if not in connection
     */
    private String databaseName;

    /**
     * Connection id for this event; Null if not in connection
     */
    private Long connectionId;

    /**
     * Connection name for this event; Null if not in connection
     */
    private String connectionName;

    /**
     * Connection host for this event; Null if not in connection
     */
    private String connectionHost;

    /**
     * Connection port for this event; Null if not in connection
     */
    private Integer connectionPort;

    /**
     * Connection cluster name for this event; Null if not in connection
     */
    private String connectionClusterName;

    /**
     * Connection tenant name for this event; Null if not in connection
     */
    private String connectionTenantName;

    /**
     * Connection database username for this event; Null if not in connection
     */
    private String connectionUsername;

    /**
     * Connection dialect type for this event; Null if not in connection
     */
    private DialectType connectionDialectType;

    /**
     * Client IP Address for this event
     */
    private String clientIpAddress;

    /**
     * ODC Server IP Address for this event
     */
    private String serverIpAddress;

    /**
     * Event detail which is a json string
     */
    private String detail;

    /**
     * Event result which could be SUCCESS/FAIL/UNKNOWN
     */
    private AuditEventResult result;

    /**
     * ID of the user who trigger this event
     */
    private Long userId;

    /**
     * Name of the user who triggers this event
     */
    private String username;

    /**
     * Organization ID of the user who trigger this event
     */
    private Long organizationId;

    /**
     * Async task id, null if not async task
     */
    private String taskId;

    /**
     * Event start time
     */
    private Date startTime;

    /**
     * Event end time
     */
    private Date endTime;
}
