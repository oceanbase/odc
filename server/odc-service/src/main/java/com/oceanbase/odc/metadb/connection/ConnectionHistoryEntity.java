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

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2021/12/31 下午4:05
 * @Description: [Entity for recording connect session history]
 */
@Data
public class ConnectionHistoryEntity {
    private Long id;
    private Date createTime;
    private Date updateTime;
    private Long connectionId;
    private Long userId;
    private Long organizationId;
    private Date lastAccessTime;

    public static ConnectionHistoryEntity of(Long connectionId, Long userId, Date lastAccessTime) {
        ConnectionHistoryEntity entity = new ConnectionHistoryEntity();
        entity.setConnectionId(connectionId);
        entity.setUserId(userId);
        entity.setLastAccessTime(lastAccessTime);
        return entity;
    }
}
