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
package com.oceanbase.odc.metadb.iam;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginHistoryRepository
        extends JpaRepository<LoginHistoryEntity, Long>, JpaSpecificationExecutor<LoginHistoryEntity> {

    /**
     * sample sql stmt: <br>
     * select user_id, max(login_time) as login_time from iam_login_history <br>
     * where user_id in (1,2) and is_success = 1 group by user_id;
     * 
     * @param userIds userId List
     * @return
     */
    @Query("select NEW com.oceanbase.odc.metadb.iam.LastSuccessLoginHistory(e.userId, max(e.loginTime) as "
            + "lastLoginTime) FROM LoginHistoryEntity e where e.userId in (:userIds) and e.success=1 group by "
            + "e.userId order by max(e.loginTime)")
    List<LastSuccessLoginHistory> lastSuccessLoginHistoryByUserIds(@Param("userIds") List<Long> userIds);

    @Query("select NEW com.oceanbase.odc.metadb.iam.LastSuccessLoginHistory(e.userId, max(e.loginTime) as "
            + "lastLoginTime) FROM LoginHistoryEntity e where e.success=1 group by e.userId "
            + "order by max(e.loginTime) ASC")
    List<LastSuccessLoginHistory> lastSuccessLoginHistory();
}
