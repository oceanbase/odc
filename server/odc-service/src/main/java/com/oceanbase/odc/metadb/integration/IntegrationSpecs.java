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

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.service.integration.model.IntegrationType;

/**
 * @author gaoda.xy
 * @date 2023/3/30 11:28
 */
public class IntegrationSpecs {
    private static final String INTEGRATION_ID = "id";
    private static final String INTEGRATION_NAME = "name";
    private static final String INTEGRATION_TYPE = "type";
    private static final String INTEGRATION_CREATOR_ID = "creatorId";
    private static final String INTEGRATION_ENABLED = "enabled";
    private static final String INTEGRATION_ORGANIZATION_ID = "organizationId";

    public static Specification<IntegrationEntity> idIn(Collection<Long> ids) {
        return SpecificationUtil.columnIn(INTEGRATION_ID, ids);
    }

    public static Specification<IntegrationEntity> nameLike(String name) {
        return SpecificationUtil.columnLike(INTEGRATION_NAME, name);
    }

    public static Specification<IntegrationEntity> typeEqual(IntegrationType type) {
        return SpecificationUtil.columnEqual(INTEGRATION_TYPE, type);
    }

    public static Specification<IntegrationEntity> creatorIdIn(Collection<Long> creatorIds) {
        return SpecificationUtil.columnIn(INTEGRATION_CREATOR_ID, creatorIds);
    }

    public static Specification<IntegrationEntity> enabledEqual(Boolean enabled) {
        return SpecificationUtil.columnEqual(INTEGRATION_ENABLED, enabled);
    }

    public static Specification<IntegrationEntity> organizationIdEqual(Long organizationId) {
        return SpecificationUtil.columnEqual(INTEGRATION_ORGANIZATION_ID, organizationId);
    }
}
