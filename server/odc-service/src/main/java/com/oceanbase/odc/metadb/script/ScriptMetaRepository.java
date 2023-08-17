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
package com.oceanbase.odc.metadb.script;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScriptMetaRepository extends JpaSpecificationExecutor<ScriptMetaEntity>,
        JpaRepository<ScriptMetaEntity, Long> {

    @Transactional
    @Modifying
    int deleteByIdAndBucketName(Long id, String bucketName);

    Optional<ScriptMetaEntity> findByIdAndBucketName(Long id, String bucketName);

    List<ScriptMetaEntity> findByBucketName(String bucketName);

    @Transactional
    @Query("update ScriptMetaEntity as e set e.objectId=:#{#scriptMetaEntity.objectId},e.objectName=:#{#scriptMetaEntity.objectName},e.bucketName=:#{#scriptMetaEntity"
            + ".bucketName},e.contentAbstract=:#{#scriptMetaEntity.contentAbstract},e.contentLength=:#{#scriptMetaEntity.contentLength} where e"
            + ".objectId=:objectId and e.id=:#{#scriptMetaEntity.id}")
    @Modifying
    int saveAndFlushIfObjectIdRetains(@Param("scriptMetaEntity") ScriptMetaEntity scriptMetaEntity,
            @Param("objectId") String objectId);

}
