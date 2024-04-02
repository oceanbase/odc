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
package com.oceanbase.odc.metadb.dbobject;

import java.util.Collection;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;

/**
 * @author gaoda.xy
 * @date 2024/3/27 19:03
 */
public interface DBColumnRepository extends OdcJpaRepository<DBColumnEntity, Long> {

    @Transactional
    @Query(value = "select t.* from connect_database_column as t where t.database_id in (:databaseIds) and "
            + "t.name like :nameKey order by t.database_id desc, t.object_id desc LIMIT 1000;",
            nativeQuery = true)
    List<DBColumnEntity> findTop1000ByDatabaseIdInAndNameLike(@Param("databaseIds") Collection<Long> databaseIds,
            @Param("nameKey") String nameKey);

}
