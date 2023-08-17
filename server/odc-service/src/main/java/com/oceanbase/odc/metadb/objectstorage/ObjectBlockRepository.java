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
package com.oceanbase.odc.metadb.objectstorage;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjectBlockRepository extends JpaSpecificationExecutor<ObjectBlockEntity>,
        JpaRepository<ObjectBlockEntity, Long> {
    /**
     * 根据文件ID查询文件块
     *
     * @param objectId 对象 ID
     * @return 文件块信息列表
     */
    List<ObjectBlockEntity> findByObjectId(String objectId);

    /**
     * 根据文件ID和序号查询文件块
     *
     * @param objectId 对象 ID
     * @param index 文件块序号
     * @return 文件块信息
     */
    Optional<ObjectBlockEntity> findByObjectIdAndIndex(String objectId, Long index);

    /**
     *
     * 根据对象 id 和文件编号查询文件块 id.
     *
     * @param objectId 存储对象 id
     * @param index 块索引
     * @return 文件块 id
     */
    @Query("SELECT id FROM ObjectBlockEntity e WHERE e.objectId=:objectId AND e.index=:index")
    long findIdByObjectIdAndIndex(@Param("objectId") String objectId, @Param("index") long index);

    /**
     * 根据存储对象 id 查询文件块的最近更新时间.
     *
     * @param objectId 存储对象 id
     * @return 文件块最新的修改时间
     */
    @Query("SELECT MAX(e.updateTime) FROM ObjectBlockEntity e WHERE e.objectId=:objectId")
    Optional<Date> findMaxUpdateTimeByObjectId(@Param("objectId") String objectId);

    /**
     * 根据文件ID删除文件块
     *
     * @param objectId 对象 ID
     * @return deleted rows count
     */
    @Transactional(rollbackOn = Exception.class)
    @Modifying
    @Query("DELETE FROM ObjectBlockEntity e WHERE e.objectId=:objectId")
    int deleteByObjectId(@Param("objectId") String objectId);

    /**
     * 使用文件id列表删除文件记录
     *
     * @param objectIds 文件唯一id
     * @return 删除记录数量
     */
    @Modifying
    @Query("DELETE FROM ObjectBlockEntity e WHERE e.objectId IN (:objectIds)")
    int deleteAllByObjectIdsIn(@Param("objectIds") Collection<String> objectIds);
}
