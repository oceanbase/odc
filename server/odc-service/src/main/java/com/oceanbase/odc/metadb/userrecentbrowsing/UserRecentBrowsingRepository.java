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
package com.oceanbase.odc.metadb.userrecentbrowsing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRecentBrowsingRepository extends JpaRepository<UserRecentBrowsingEntity, Long> {

    UserRecentBrowsingEntity findByProjectIdAndUserIdAndItemTypeAndItemId(Long projectId, Long userId, String itemType,
            Long itemId);

    @Query(value = "SELECT * FROM odc_user_recent_browsing u WHERE u.project_id = :projectId AND u.user_id = :userId "
            + "AND u.item_type = :itemType ORDER BY u.browse_time DESC, u.id DESC LIMIT :limit",
            nativeQuery = true)
    List<UserRecentBrowsingEntity> listRecentBrowsingWithLimit(@Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("itemType") String itemType,
            @Param("limit") Integer limit);

}
