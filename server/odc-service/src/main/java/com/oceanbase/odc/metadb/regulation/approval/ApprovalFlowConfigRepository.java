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
package com.oceanbase.odc.metadb.regulation.approval;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalFlowConfigRepository extends JpaRepository<ApprovalFlowConfigEntity, Long>,
        JpaSpecificationExecutor<ApprovalFlowConfigEntity> {
    List<ApprovalFlowConfigEntity> findByOrganizationId(Long organizationId);

    Optional<ApprovalFlowConfigEntity> findByOrganizationIdAndId(Long organizationId, Long id);

    @Query(value = "select distinct(f_c.*) from regulation_approval_flow_config f_c inner join regulation_approval_flow_node_config"
            + " f_n_c on f_c.id = f_n_c.approval_flow_config_id where f_n_c.external_approval_id = :integrationId",
            nativeQuery = true)
    List<ApprovalFlowConfigEntity> findByIntegrationId(@Param("integrationId") Long integrationId);

}
