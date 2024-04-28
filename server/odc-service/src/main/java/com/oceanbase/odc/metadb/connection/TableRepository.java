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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * ClassName: TableRepository Package: com.oceanbase.odc.metadb.connection Description:
 *
 * @Author: fenghao
 * @Create 2024/3/12 20:00
 * @Version 1.0
 */
@Repository
public interface TableRepository extends JpaRepository<TableEntity, Long>, JpaSpecificationExecutor<TableEntity> {
    List<TableEntity> findByDatabaseId(Long databaseId);

    List<TableEntity> findByDatabaseIdIn(Set<Long> databaseId);

    TableEntity findByName(String name);

    TableEntity findByDatabaseIdAndName(Long databaseId, String name);

}
