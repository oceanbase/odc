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

import java.util.Objects;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder.In;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.core.shared.constant.ResourceType;

/**
 * Specification for repository <code>ResourceGroupConnectionRepository</code>
 *
 * @author yh263208
 * @date 2021-07-27 16:23
 * @since ODC_release_3.2.0
 */
public class ResourceGroupConnectionSpecs {
    /**
     * Field name for <code>ResourceGroupConnectionEntity.connectionId</code>
     */
    private final static String RESOURCE_GROUP_CONNECTION_RESOURCEID_NAME = "resourceId";
    /**
     * Field name for <code>ResourceGroupConnectionEntity.resourceGroupId</code>
     */
    private final static String RESOURCE_GROUP_CONNECTION_RESOURCEGROUPID_NAME = "resourceGroupId";
    /**
     * Field name for <code>ResourceGroupConnectionEntity.creatorId</code>
     */
    private final static String RESOURCE_GROUP_CONNECTION_CREATORID_NAME = "creatorId";
    /**
     * Field name for <code>ResourceGroupConnectionEntity.resourceType</code>
     */
    private final static String RESOURCE_GROUP_CONNECTION_RESOURCETYPE_NAME = "resourceType";

    public static Specification<ResourceGroupConnectionEntity> resourceIdEqual(Long resourceId) {
        return columnEqualSepc(RESOURCE_GROUP_CONNECTION_RESOURCEID_NAME, resourceId);
    }

    public static Specification<ResourceGroupConnectionEntity> resourceTypeEqual(ResourceType resourceType) {
        return columnEqualSepc(RESOURCE_GROUP_CONNECTION_RESOURCETYPE_NAME,
                resourceType == null ? null : resourceType.name());
    }

    public static Specification<ResourceGroupConnectionEntity> resourceGroupIdEqual(Long resourceGroupId) {
        return columnEqualSepc(RESOURCE_GROUP_CONNECTION_RESOURCEGROUPID_NAME, resourceGroupId);
    }

    public static Specification<ResourceGroupConnectionEntity> creatorIdEqual(Long creatorId) {
        return columnEqualSepc(RESOURCE_GROUP_CONNECTION_CREATORID_NAME, creatorId);
    }

    public static Specification<ResourceGroupConnectionEntity> resourceIdIn(Set<Long> resourceIds) {
        return (Specification<ResourceGroupConnectionEntity>) (root, query, builder) -> {
            if (CollectionUtils.isEmpty(resourceIds)) {
                return builder.conjunction();
            }
            In<Long> inLong = builder.in(root.get(RESOURCE_GROUP_CONNECTION_RESOURCEID_NAME));
            for (Long resourceId : resourceIds) {
                inLong.value(resourceId);
            }
            return inLong;
        };
    }

    public static Specification<ResourceGroupConnectionEntity> resourceGroupIdIn(Set<Long> resourceGroupIds) {
        return (Specification<ResourceGroupConnectionEntity>) (root, query, builder) -> {
            if (CollectionUtils.isEmpty(resourceGroupIds)) {
                return builder.conjunction();
            }
            In<Long> inLong = builder.in(root.get(RESOURCE_GROUP_CONNECTION_RESOURCEGROUPID_NAME));
            for (Long resourceGroupId : resourceGroupIds) {
                inLong.value(resourceGroupId);
            }
            return inLong;
        };
    }

    /**
     * Generate a column equal predicate logic
     *
     * @param columnName fieldName for <code>ResourceGroupConnectionEntity</code>
     * @param value value for predicate
     * @return generated <code>Specification</code>
     */
    private static Specification<ResourceGroupConnectionEntity> columnEqualSepc(String columnName, Object value) {
        Validate.notNull(columnName, "ColumnName can not be null for ResourceGroupConnectionSpec#columnEqualSepc");
        return (Specification<ResourceGroupConnectionEntity>) (root, query, builder) -> {
            if (Objects.isNull(value)) {
                return builder.conjunction();
            }
            return builder.equal(root.get(columnName), value);
        };
    }

}
