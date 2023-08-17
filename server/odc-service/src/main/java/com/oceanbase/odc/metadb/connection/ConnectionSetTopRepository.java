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

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link ConnectionSetTopRepository}
 *
 * @author yh263208
 * @date 2022-11-30 15:28
 * @since ODC_release_4.1.0
 */
public interface ConnectionSetTopRepository
        extends JpaRepository<ConnectionSetTopEntity, Long>, JpaSpecificationExecutor<ConnectionSetTopEntity> {

    List<ConnectionSetTopEntity> findByUserId(Long userId);

    @Transactional
    @Modifying
    @Query(value = "delete from connect_connection_set_top where user_id=?1", nativeQuery = true)
    int deleteByUserId(Long userId);

    @Transactional
    @Modifying
    @Query(value = "delete from connect_connection_set_top where connection_id=?1", nativeQuery = true)
    int deleteByConnectionId(Long connectionId);

    @Transactional
    @Modifying
    @Query(value = "delete from connect_connection_set_top where connection_id in (?1)", nativeQuery = true)
    int deleteByConnectionIds(Set<Long> connectionIds);

    @Transactional
    @Modifying
    @Query(value = "delete from connect_connection_set_top where user_id=?1 and connection_id=?2", nativeQuery = true)
    int deleteByUserIdAndConnectionId(Long userId, Long connectionId);

}
