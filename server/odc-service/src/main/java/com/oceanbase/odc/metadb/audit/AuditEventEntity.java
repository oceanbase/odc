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
package com.oceanbase.odc.metadb.audit;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/1/17 下午7:38
 * @Description: []
 */
@Data
@Entity
@Table(name = "audit_event")
public class AuditEventEntity {
    /**
     * auto-increase primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * record create time
     */
    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private Date createTime;

    /**
     * record last update time
     */
    @Column(name = "update_time", nullable = false, insertable = false, updatable = false)
    private Date updateTime;

    /**
     * Audit event type
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AuditEventType type;

    /**
     * Audit event action
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditEventAction action;

    /**
     * Database id for this event
     */
    @Column(name = "database_id")
    private Long databaseId;

    /**
     * Database name for this event; Null if not in connection
     */
    @Column(name = "database_name")
    private String databaseName;

    /**
     * Connection id for this event; Null if not in connection
     */
    @Column(name = "connection_id")
    private Long connectionId;

    /**
     * Connection name for this event; Null if not in connection
     */
    @Column(name = "connection_name")
    private String connectionName;

    /**
     * Connection host for this event; Null if not in connection
     */
    @Column(name = "connection_host")
    private String connectionHost;

    /**
     * Connection port for this event; Null if not in connection
     */
    @Column(name = "connection_port")
    private Integer connectionPort;

    /**
     * Connection cluster for this event; Null if not in connection
     */
    @Column(name = "connection_cluster_name")
    private String connectionClusterName;

    /**
     * Connection tenant for this event; Null if not in connection
     */
    @Column(name = "connection_tenant_name")
    private String connectionTenantName;

    /**
     * Connection schema for this event; Null if not in connection
     */
    @Column(name = "connection_username")
    private String connectionUsername;

    /**
     * Connection dialect type for this event; Null if not in connection
     */
    @Column(name = "connection_dialect_type")
    @Enumerated(EnumType.STRING)
    private DialectType connectionDialectType;

    /**
     * Client IP Address for this event
     */
    @Column(name = "client_ip_address")
    private String clientIpAddress;

    /**
     * ODC Server IP Address for this event
     */
    @Column(name = "server_ip_address")
    private String serverIpAddress;

    /**
     * Event detail which is a json string
     */
    @Column(name = "detail", nullable = false)
    private String detail;

    /**
     * Event result which could be SUCCESS/FAIL/UNKNOWN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private AuditEventResult result;

    /**
     * ID of the user who triggers this event
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Name of the user who triggers this event
     */
    @Column(name = "username", nullable = false)
    private String username;

    /**
     * Organization ID of the user who trigger this event
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Async task id, null if not async task
     */
    @Column(name = "task_id")
    private String taskId;

    /**
     * Event start time
     */
    @Column(name = "start_time")
    private Date startTime;

    /**
     * Event end time
     */
    @Column(name = "end_time")
    private Date endTime;
}
