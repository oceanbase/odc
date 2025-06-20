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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;

import lombok.NonNull;

/**
 * @Author: ysj
 * @Date: 2025/2/24 11:28
 * @Since: 4.3.4
 * @Description: database access history repository
 */
public interface DatabaseAccessHistoryRepository extends OdcJpaRepository<DatabaseAccessHistoryEntity, Long>,
        JpaSpecificationExecutor<DatabaseAccessHistoryEntity> {

    Page<DatabaseAccessHistoryEntity> findByUserId(Long userId, Pageable pageable);

    @Transactional
    default int upsert(@NonNull Collection<DatabaseAccessHistoryEntity> historyEntities) {
        List<DatabaseAccessHistoryEntity> histories = historyEntities.stream().filter(Objects::nonNull).collect(
                Collectors.toList());
        if (histories.isEmpty()) {
            return 0;
        }
        String sql = "INSERT INTO database_access_history " +
                "(user_id, database_id, last_access_time, connection_id) " +
                "VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE " +
                "last_access_time = VALUES(last_access_time)";
        return getJdbcTemplate().batchUpdate(sql, historyEntities.stream().map(h -> new Object[] {
                h.getUserId(),
                h.getDatabaseId(),
                h.getLastAccessTime(),
                h.getConnectionId()
        }).collect(Collectors.toList())).length;
    }
}
