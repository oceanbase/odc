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
package com.oceanbase.odc.metadb.connection;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.connection.model.ConnectionSyncErrorReason;
import com.oceanbase.odc.service.connection.model.ConnectionSyncResult;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/11/6 15:57
 * @Description: []
 */
@Data
@Entity
@Table(name = "connect_connection_sync_history")
public class ConnectionSyncHistoryEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "organization_id", updatable = false)
    private Long organizationId;

    @Column(name = "connection_id", updatable = false)
    private Long connectionId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "last_sync_result")
    private ConnectionSyncResult lastSyncResult;

    @Column(name = "last_sync_time")
    private Date lastSyncTime;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "last_sync_error_reason")
    private ConnectionSyncErrorReason lastSyncErrorReason;

    @Column(name = "last_sync_error_message")
    private String lastSyncErrorMessage;
}
