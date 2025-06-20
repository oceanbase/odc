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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.model.User;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: ysj
 * @Date: 2025/2/24 15:25
 * @Since: 4.3.4
 * @Description:
 */
@Data
@Entity
@EqualsAndHashCode(exclude = {"createTime", "updateTime"})
@Table(name = "database_access_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "database_id"})
})
public class DatabaseAccessHistoryEntity {

    public static final String LAST_ACCESS_TIME_NAME = "lastAccessTime";

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Refer to {@link ConnectionConfig#getId()}
     */
    @Column(name = "connection_id")
    private Long connectionId;

    /**
     * Refer to {@link User#getId()}
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Refer to {@link Database#getId()}
     */
    @Column(name = "database_id", nullable = false)
    private Long databaseId;

    @Column(name = "last_access_time", nullable = false)
    private Date lastAccessTime;

    @Generated(GenerationTime.INSERT)
    @Column(name = "create_time", insertable = false, updatable = false,
            columnDefinition = "datetime NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false,
            columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Date updateTime;

}
