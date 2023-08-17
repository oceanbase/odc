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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.common.util.StringUtils;

/**
 * <code>Specification</code> object for JPA query, used to construct predicate logic
 *
 * @author yh263208
 * @date 2021-07-26 16:54
 * @since ODC_release_3.2.0
 */
public class ResourceGroupSpecs {
    /**
     * Field name for <code>ResourceGroupEntity.id</code>
     */
    private final static String RESOURCE_GROUP_ID_NAME = "id";
    /**
     * Field name for <code>ResourceGroupEntity.name</code>
     */
    private final static String RESOURCE_GROUP_NAME_NAME = "name";
    /**
     * Field name for <code>ResourceGroupEntity.userId</code>
     */
    private final static String RESOURCE_GROUP_USERID_NAME = "creatorId";
    /**
     * Field name for <code>ResourceGroupEntity.organizationId</code>
     */
    private final static String RESOURCE_GROUP_ORGANIZATIONID_NAME = "organizationId";
    /**
     * Field name for <code>ResourceGroupEntity.createTime</code>
     */
    private final static String RESOURCE_GROUP_CREATETIME_NAME = "createTime";
    /**
     * Field name for <code>ResourceGroupEntity.updateTime</code>
     */
    private final static String RESOURCE_GROUP_UPDATETIME_NAME = "updateTime";
    /**
     * Field name for <code>ResourceGroupEntity.enabled</code>
     */
    private final static String RESOURCE_GROUP_ENABLED_NAME = "enabled";

    public static Specification<ResourceGroupEntity> idEqual(Long id) {
        return columnEqualSpec(RESOURCE_GROUP_ID_NAME, id);
    }

    public static Specification<ResourceGroupEntity> idIn(Collection<Long> ids) {
        return SpecificationUtil.columnIn(RESOURCE_GROUP_ID_NAME, ids);
    }

    public static Specification<ResourceGroupEntity> nameEqual(String name) {
        return columnEqualSpec(RESOURCE_GROUP_NAME_NAME, name);
    }

    public static Specification<ResourceGroupEntity> nameLike(String nameLike) {
        return (Specification<ResourceGroupEntity>) (root, query, builder) -> {
            if (StringUtils.isBlank(nameLike)) {
                return builder.conjunction();
            }
            return builder.like(root.get(RESOURCE_GROUP_NAME_NAME), "%" + StringUtils.escapeLike(nameLike) + "%");
        };
    }

    public static Specification<ResourceGroupEntity> idLike(String idLike) {
        return (Specification<ResourceGroupEntity>) (root, query, builder) -> {
            if (StringUtils.isBlank(idLike)) {
                return builder.conjunction();
            }
            return builder.like(root.get(RESOURCE_GROUP_ID_NAME).as(String.class),
                    "%" + StringUtils.escapeLike(idLike) + "%");
        };
    }

    public static Specification<ResourceGroupEntity> creatorIdEqual(Long userId) {
        return columnEqualSpec(RESOURCE_GROUP_USERID_NAME, userId);
    }

    public static Specification<ResourceGroupEntity> organizationIdEqual(Long organizationId) {
        return columnEqualSpec(RESOURCE_GROUP_ORGANIZATIONID_NAME, organizationId);
    }

    public static Specification<ResourceGroupEntity> createTimeEqual(Date createDate) {
        return columnEqualSpec(RESOURCE_GROUP_CREATETIME_NAME, createDate);
    }

    public static Specification<ResourceGroupEntity> createTimeLate(Date createDate) {
        return dateLateSpec(RESOURCE_GROUP_CREATETIME_NAME, createDate);
    }

    public static Specification<ResourceGroupEntity> createTimeEarly(Date createDate) {
        return dateEarlySpec(RESOURCE_GROUP_CREATETIME_NAME, createDate);
    }

    public static Specification<ResourceGroupEntity> updateTimeEqual(Date updateDate) {
        return columnEqualSpec(RESOURCE_GROUP_UPDATETIME_NAME, updateDate);
    }

    public static Specification<ResourceGroupEntity> updateTimeLate(Date updateDate) {
        return dateLateSpec(RESOURCE_GROUP_UPDATETIME_NAME, updateDate);
    }

    public static Specification<ResourceGroupEntity> updateTimeEarly(Date updateDate) {
        return dateEarlySpec(RESOURCE_GROUP_UPDATETIME_NAME, updateDate);
    }

    public static Specification<ResourceGroupEntity> enabled() {
        return columnEqualSpec(RESOURCE_GROUP_ENABLED_NAME, true);
    }

    public static Specification<ResourceGroupEntity> status(Boolean status) {
        return columnEqualSpec(RESOURCE_GROUP_ENABLED_NAME, status);
    }

    public static Specification<ResourceGroupEntity> statusIn(List<Boolean> statuses) {
        return (root, query, builder) -> {
            if (CollectionUtils.isEmpty(statuses)) {
                return builder.conjunction();
            } else {
                return root.get(RESOURCE_GROUP_ENABLED_NAME).in(statuses);
            }
        };
    }

    public static Specification<ResourceGroupEntity> disabled() {
        return columnEqualSpec(RESOURCE_GROUP_ENABLED_NAME, false);
    }

    private static Specification<ResourceGroupEntity> dateLateSpec(String columnName, Date columnValue) {
        Validate.notNull(columnName, "ColumnName can not be null for ResourceGroupSpec#dateBeforeSpecification");
        Validate.notNull(columnValue, "ColumnValue can not be null for ResourceGroupSpec#dateBeforeSpecification");
        return (Specification<ResourceGroupEntity>) (root, query, builder) -> builder.lessThan(root.get(columnName),
                columnValue);
    }

    private static Specification<ResourceGroupEntity> dateEarlySpec(String columnName, Date columnValue) {
        Validate.notNull(columnName, "ColumnName can not be null for ResourceGroupSpec#dateBeforeSpecification");
        Validate.notNull(columnValue, "ColumnValue can not be null for ResourceGroupSpec#dateBeforeSpecification");
        return (Specification<ResourceGroupEntity>) (root, query, builder) -> builder.greaterThan(root.get(columnName),
                columnValue);
    }

    private static Specification<ResourceGroupEntity> columnEqualSpec(String columnName, Object columnValue) {
        Validate.notNull(columnName, "ColumnName can not be null for ResourceGroupSpec#columnSpecification");
        return (Specification<ResourceGroupEntity>) (root, query, builder) -> {
            if (Objects.isNull(columnValue)) {
                return builder.conjunction();
            }
            return builder.equal(root.get(columnName), columnValue);
        };
    }

}
