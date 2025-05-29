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

import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.common.util.EmptyValues;

/**
 * @author gaoda.xy
 * @date 2022/12/6 16:41
 */
public class UserSpecs {

    public static Specification<UserLoginViewEntity> enabledEqual(Boolean enabled) {
        return columnEqual("enabled", enabled);
    }

    public static Specification<UserLoginViewEntity> organizationIdEqual(Long organizationId) {
        return columnEqual("organizationId", organizationId);
    }

    public static Specification<UserLoginViewEntity> userIdIn(List<Long> userIds) {
        return ((root, query, criteriaBuilder) -> criteriaBuilder.and(root.get("id").in(userIds)));
    }

    public static Specification<UserLoginViewEntity> namesLike(List<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return (root, query, builder) -> builder.conjunction();
        }
        String field = "name";
        return names.stream().filter(StringUtils::isNotBlank).map(s -> {
            if (EmptyValues.matches(s)) {
                return columnEqualsNull(field).or(columnEqual(field, EmptyValues.STRING_VALUE));
            }
            return UserSpecs.nameLike(s);
        }).reduce(Specification::or).orElseGet(() -> (root, query, builder) -> builder.conjunction());
    }

    public static Specification<UserLoginViewEntity> nameLike(String name) {
        return (root, query, builder) -> StringUtils.isBlank(name) ? builder.conjunction()
                : builder.like(root.get("name").as(String.class), "%" + StringUtils.escapeLike(name) + "%");
    }

    public static Specification<UserLoginViewEntity> accountNamesLike(List<String> accountNames) {
        if (CollectionUtils.isEmpty(accountNames)) {
            return (root, query, builder) -> builder.conjunction();
        }
        String field = "accountName";
        return accountNames.stream().filter(StringUtils::isNotBlank).map(s -> {
            if (EmptyValues.matches(s)) {
                return columnEqualsNull(field).or(columnEqual(field, EmptyValues.STRING_VALUE));
            }
            return UserSpecs.accountNameLike(s);
        }).reduce(Specification::or).orElseGet(() -> (root, query, builder) -> builder.conjunction());
    }

    public static Specification<UserLoginViewEntity> accountNameLike(String accountName) {
        return (root, query, builder) -> StringUtils.isBlank(accountName) ? builder.conjunction()
                : builder.like(root.get("accountName").as(String.class),
                        "%" + StringUtils.escapeLike(accountName) + "%");
    }

    private static Specification<UserLoginViewEntity> columnEqualsNull(String column) {
        return (root, query, builder) -> builder.isNull(root.get(column));
    }

    private static Specification<UserLoginViewEntity> columnEqual(String column, Object value) {
        return (root, query, builder) -> {
            if (Objects.isNull(value)) {
                return builder.conjunction();
            } else {
                return builder.equal(root.get(column), value);
            }
        };
    }
}
