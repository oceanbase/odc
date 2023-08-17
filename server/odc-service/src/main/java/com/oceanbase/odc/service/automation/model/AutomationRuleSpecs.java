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
package com.oceanbase.odc.service.automation.model;

import java.util.Collection;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.automation.AutomationRuleEntity;

public class AutomationRuleSpecs {

    public static Specification<AutomationRuleEntity> valueLike(String columnName, String valueLike) {
        return (root, query, builder) -> {
            if (StringUtils.isBlank(valueLike)) {
                return builder.conjunction();
            }
            return builder.like(root.get(columnName), "%" + StringUtils.escapeLike(valueLike) + "%");
        };
    }

    public static Specification<AutomationRuleEntity> valueEquals(String columnName, Object value) {
        return (root, query, builder) -> {
            if (Objects.isNull(value)) {
                return builder.conjunction();
            }
            return builder.equal(root.get(columnName), value);
        };
    }

    public static Specification<AutomationRuleEntity> valueIn(String columnName, Collection<?> values) {
        return (root, query, builder) -> {
            if (CollectionUtils.isEmpty(values)) {
                return builder.conjunction();
            } else {
                return root.get(columnName).in(values);
            }
        };
    }

}
