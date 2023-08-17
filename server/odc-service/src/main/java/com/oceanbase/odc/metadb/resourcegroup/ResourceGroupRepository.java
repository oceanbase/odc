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
package com.oceanbase.odc.metadb.resourcegroup;

import java.util.Collection;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data Access Object for <code>ResourceGroupEntity</code>
 *
 * @author yh263208
 * @date 2021-07-27 11:29
 * @since ODC_release_3.2.0
 */
public interface ResourceGroupRepository
        extends JpaRepository<ResourceGroupEntity, Long>, JpaSpecificationExecutor<ResourceGroupEntity> {
    @Transactional
    @Query("update ResourceGroupEntity as rg set rg.name=:#{#param.name},rg.description=:#{#param.description},rg.enabled=:#{#param.enabled},rg.lastModifierId=:#{#param.lastModifierId} where rg.id=:#{#param.id}")
    @Modifying
    int updateById(@Param("param") ResourceGroupEntity entity);

    @Query(value = "select distinct(rg.*) from (select * from connect_connection where visible_scope='ORGANIZATION' "
            + "and id=:connectionId) as c left join (select * from iam_resource_group_resource where "
            + "resource_type='ODC_CONNECTION') as rgr on rgr.resource_id=c.id left join iam_resource_group "
            + "as rg on rgr.resource_group_id=rg.id", nativeQuery = true)
    List<ResourceGroupEntity> findByConnectionId(@Param("connectionId") Long id);

    List<ResourceGroupEntity> findByOrganizationId(Long organizationId);

    List<ResourceGroupEntity> findByIdIn(Collection<Long> ids);
}
