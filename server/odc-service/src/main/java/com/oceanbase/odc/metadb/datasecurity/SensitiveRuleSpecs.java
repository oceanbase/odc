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
package com.oceanbase.odc.metadb.datasecurity;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;

/**
 * @author gaoda.xy
 * @date 2023/5/19 10:57
 */
public class SensitiveRuleSpecs {

    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String ENABLED = "enabled";
    private static final String PROJECT_ID = "projectId";
    private static final String ORGANIZATION_ID = "organizationId";
    private static final String MASKING_ALGORITHM_ID = "maskingAlgorithmId";

    public static Specification<SensitiveRuleEntity> typeIn(Collection<SensitiveRuleType> types) {
        return SpecificationUtil.columnIn(TYPE, types);
    }

    public static Specification<SensitiveRuleEntity> nameLike(String name) {
        return SpecificationUtil.columnLike(NAME, name);
    }

    public static Specification<SensitiveRuleEntity> enabledEqual(Boolean enabled) {
        return SpecificationUtil.columnEqual(ENABLED, enabled);
    }

    public static Specification<SensitiveRuleEntity> projectIdEqual(Long projectId) {
        return SpecificationUtil.columnEqual(PROJECT_ID, projectId);
    }

    public static Specification<SensitiveRuleEntity> organizationIdEqual(Long organizationId) {
        return SpecificationUtil.columnEqual(ORGANIZATION_ID, organizationId);
    }

    public static Specification<SensitiveRuleEntity> maskingAlgorithmIdIn(Collection<Long> maskingAlgorithmIds) {
        return SpecificationUtil.columnIn(MASKING_ALGORITHM_ID, maskingAlgorithmIds);
    }

}
