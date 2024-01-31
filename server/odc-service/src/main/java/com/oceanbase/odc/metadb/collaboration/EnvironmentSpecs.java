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
package com.oceanbase.odc.metadb.collaboration;

import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

/**
 * @Author: Lebie
 * @Date: 2024/1/30 18:58
 * @Description: []
 */
public class EnvironmentSpecs {
    public static Specification<EnvironmentEntity> organizationIdEquals(Long organizationId) {
        return columnEquals("organizationId", organizationId);
    }

    public static Specification<EnvironmentEntity> enabledEquals(Boolean enabled) {
        return columnEquals("enabled", enabled);
    }

    private static Specification<EnvironmentEntity> columnEquals(String column, Object value) {
        return (root, query, builder) -> {
            if (Objects.isNull(value)) {
                return builder.conjunction();
            } else {
                return builder.equal(root.get(column), value);
            }
        };
    }
}
