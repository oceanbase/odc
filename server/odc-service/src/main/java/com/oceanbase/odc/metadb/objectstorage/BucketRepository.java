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

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BucketRepository extends JpaSpecificationExecutor<BucketEntity>,
        JpaRepository<BucketEntity, Long> {
    /**
     * 使用存储空间名称查询存储空间对象.
     *
     * @param name 存储空间
     * @return 存储空间对象
     */
    Optional<BucketEntity> findByName(String name);

    /**
     * 根据 bucket 名称删除 bucket.
     *
     * @param name bucket 名称
     */
    @Transactional(rollbackOn = Exception.class)
    void deleteByName(String name);
}
