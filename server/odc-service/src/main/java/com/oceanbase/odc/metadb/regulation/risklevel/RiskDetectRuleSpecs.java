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
package com.oceanbase.odc.metadb.regulation.risklevel;

import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2023/6/15 21:15
 * @Description: []
 */
public class RiskDetectRuleSpecs {
    public static Specification<RiskDetectRuleEntity> nameLike(String name) {
        return (root, query, builder) -> StringUtils.isBlank(name) ? builder.conjunction()
                : builder.like(root.get("name"), "%" + StringUtils.escapeLike(name) + "%");
    }

    public static Specification<RiskDetectRuleEntity> organizationIdEqual(Long organizationId) {
        return (root, query, builder) -> Objects.isNull(organizationId) ? builder.conjunction()
                : builder.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<RiskDetectRuleEntity> riskLevelIdEqual(Long riskLevelId) {
        return (root, query, builder) -> Objects.isNull(riskLevelId) ? builder.conjunction()
                : builder.equal(root.get("riskLevelId"), riskLevelId);
    }

}
