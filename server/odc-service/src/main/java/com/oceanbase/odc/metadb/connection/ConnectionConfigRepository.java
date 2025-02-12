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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;

public interface ConnectionConfigRepository
        extends JpaRepository<ConnectionEntity, Long>, JpaSpecificationExecutor<ConnectionEntity> {

    List<ConnectionEntity> findByUpdateTimeBefore(Date updateTime);

    List<ConnectionEntity> findByUpdateTimeBeforeAndTemp(Date updateTime, Boolean temp);

    List<ConnectionEntity> findByOrganizationIdAndNameIn(Long organizationId, Collection<String> names);

    Optional<ConnectionEntity> findByVisibleScopeAndOwnerIdAndName(ConnectionVisibleScope visibleScope, Long ownerId,
            String name);

    Optional<ConnectionEntity> findByOrganizationIdAndName(Long organizationId, String name);

    List<ConnectionEntity> findByOrganizationId(Long organizationId);

    List<ConnectionEntity> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<ConnectionEntity> findByOrganizationIdIn(Collection<Long> organizationIds);

    List<ConnectionEntity> findByProjectId(Long projectId);

    List<ConnectionEntity> findByOrganizationIdAndClusterName(Long organizationId, String clusterName);

    List<ConnectionEntity> findByOrganizationIdAndTenantName(Long organizationId, String tenantName);


    @Transactional
    @Query(value = "select distinct(c_c.*) from `connect_connection` as c_c inner join `connect_database` as c_d "
            + "on c_c.id = c_d.connection_id where c_d.project_id = :projectId",
            nativeQuery = true)
    List<ConnectionEntity> findByDatabaseProjectId(@Param("projectId") Long projectId);

    @Transactional
    @Query(value = "select `id` from `connect_connection` where `host`=:host", nativeQuery = true)
    @Modifying
    Set<Long> findIdsByHost(@Param("host") String host);

    List<ConnectionEntity> findByIdIn(Collection<Long> ids);

    List<ConnectionEntity> findByVisibleScopeAndOrganizationId(ConnectionVisibleScope visibleScope,
            Long organizationId);

    List<ConnectionEntity> findByVisibleScopeAndCreatorId(ConnectionVisibleScope visibleScope,
            Long creatorId);

    List<ConnectionEntity> findByVisibleScope(ConnectionVisibleScope visibleScope);

    List<ConnectionEntity> findByOrganizationIdAndEnvironmentId(Long organizationId, Long environmentId);

    @Transactional
    int deleteByVisibleScopeAndOwnerId(ConnectionVisibleScope visibleScope, Long ownerId);

    @Query("SELECT e.id FROM #{#entityName} e"
            + " WHERE e.visibleScope=:visibleScope AND e.organizationId=:organizationId")
    Set<Long> findIdsByVisibleScopeAndOrganizationId(@Param("visibleScope") ConnectionVisibleScope visibleScope,
            @Param("organizationId") Long organizationId);

    @Query("SELECT e.id FROM #{#entityName} e"
            + " WHERE e.organizationId=:organizationId")
    Set<Long> findIdsByOrganizationId(@Param("organizationId") Long organizationId);

    @Query(value = "update connect_connection set `password`=:passwordEncyrpted,"
            + " sys_tenant_password=:sysTenantPasswordEncyrpted,"
            + " readonly_password=:readonlyPasswordEncyrpted where `id`=:connectionId", nativeQuery = true)
    @Modifying
    @Transactional
    int updatePasswordsById(@Param("connectionId") Long connectionId,
            @Param("passwordEncyrpted") String passwordEncyrpted,
            @Param("sysTenantPasswordEncyrpted") String sysTenantPasswordEncyrpted,
            @Param("readonlyPasswordEncyrpted") String readonlyPasswordEncyrpted);

    @Transactional
    @Query(value = "delete from `connect_connection` where id in (:ids)", nativeQuery = true)
    @Modifying
    int deleteByIds(@Param("ids") Set<Long> ids);

    @Query(value = "SELECT cc.* FROM connect_connection cc " +
            "LEFT JOIN connect_connection_sync_history csh ON cc.id = csh.connection_id " +
            "WHERE cc.visible_scope = 'ORGANIZATION' AND ("
            + " csh.last_sync_result = 'SUCCESS'"
            + " OR csh.last_sync_error_reason IS NULL"
            + " OR csh.last_sync_error_reason != 'CLUSTER_NOT_EXISTS')",
            nativeQuery = true)
    List<ConnectionEntity> findSyncableConnections();

    @Query(value = "SELECT cc.* FROM connect_connection cc " +
            "LEFT JOIN connect_connection_sync_history csh ON cc.id = csh.connection_id " +
            "WHERE cc.organization_id IN (:organizationIds) AND cc.visible_scope = 'ORGANIZATION' AND ("
            + " csh.last_sync_result = 'SUCCESS'"
            + " OR csh.last_sync_error_reason IS NULL"
            + " OR csh.last_sync_error_reason != 'CLUSTER_NOT_EXISTS')",
            nativeQuery = true)
    List<ConnectionEntity> findSyncableConnectionsByOrganizationIdIn(
            @Param("organizationIds") Collection<Long> organizationIds);


}
