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

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.constant.AccessKeyStatus;

import jakarta.transaction.Transactional;

@Repository
public interface AccessKeyRepository
        extends OdcJpaRepository<AccessKeyEntity, Long>, JpaSpecificationExecutor<AccessKeyEntity> {


    Optional<AccessKeyEntity> findByAccessKeyIdAndStatusNot(String accessKeyId, AccessKeyStatus status);

    Optional<AccessKeyEntity> findByAccessKeyId(String accessKeyId);

    @Modifying
    @Query("UPDATE AccessKeyEntity a SET a.status = 'DELETED' WHERE a.accessKeyId = :accessKeyId")
    @Transactional
    void softDelete(@Param("accessKeyId") String accessKeyId);

    Page<AccessKeyEntity> findByUserIdAndStatusNot(Long userId, AccessKeyStatus status, Pageable pageable);

    long countByUserIdAndStatusNot(Long userId, AccessKeyStatus status);
}
