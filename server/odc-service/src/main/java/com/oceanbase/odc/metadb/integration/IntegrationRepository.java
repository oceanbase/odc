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
package com.oceanbase.odc.metadb.integration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.integration.model.IntegrationType;

/**
 * @author gaoda.xy
 * @date 2023/3/23 19:49
 */
public interface IntegrationRepository extends OdcJpaRepository<IntegrationEntity, Long> {

    Optional<IntegrationEntity> findByNameAndTypeAndOrganizationId(String name, IntegrationType type,
            Long organizationId);

    List<IntegrationEntity> findByTypeAndEnabledAndOrganizationId(IntegrationType type, Boolean enabled,
            Long organizationId);

    List<IntegrationEntity> findByTypeAndOrganizationId(IntegrationType type, Long organizationId);

    List<IntegrationEntity> findByTypeAndEnabledAndOrganizationIdIn(IntegrationType type, Boolean enabled,
            Collection<Long> organizationIds);

    Optional<IntegrationEntity> findByTypeAndOrganizationIdAndName(IntegrationType type, Long organizationId, String name);

}
