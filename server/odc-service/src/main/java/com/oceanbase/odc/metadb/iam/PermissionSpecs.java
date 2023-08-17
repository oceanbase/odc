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
package com.oceanbase.odc.metadb.iam;

import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;

/**
 * <code>Specification</code> object for JPA query, used to construct predicate logic
 *
 * @author yh263208
 * @date 2021-08-09 14:38
 * @since ODC-release_3.2.0
 */
public class PermissionSpecs {
    /**
     * Field name for <code>PermissionEntity.type</code>
     */
    private final static String PERMISSION_TYPE_NAME = "type";
    /**
     * Field name for <code>PermissionEntity.type</code>
     */
    private final static String PERMISSION_RESOURCEIDENTIFIER_NAME = "resourceIdentifier";

    public static Specification<PermissionEntity> typeEquals(PermissionType type) {
        return columnEqualSpec(PERMISSION_TYPE_NAME, type);
    }

    public static Specification<PermissionEntity> resourceTypeEquals(ResourceType resourceType) {
        return (Specification<PermissionEntity>) (root, query, builder) -> {
            if (resourceType == null) {
                return builder.conjunction();
            }
            return builder.like(root.get(PERMISSION_RESOURCEIDENTIFIER_NAME),
                    "%" + StringUtils.escapeLike(resourceType.name()) + "%");
        };
    }

    private static Specification<PermissionEntity> columnEqualSpec(String columnName, Object columnValue) {
        Validate.notNull(columnName, "ColumnName can not be null for PermissionSpecs#columnSpecification");
        return (Specification<PermissionEntity>) (root, query, builder) -> {
            if (Objects.isNull(columnValue)) {
                return builder.conjunction();
            }
            return builder.equal(root.get(columnName), columnValue);
        };
    }

}
