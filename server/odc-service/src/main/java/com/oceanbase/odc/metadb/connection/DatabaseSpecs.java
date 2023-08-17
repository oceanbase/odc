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
package com.oceanbase.odc.metadb.connection;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2023/6/5 15:54
 * @Description: []
 */
public class DatabaseSpecs {
    public static Specification<DatabaseEntity> idIn(List<Long> ids) {
        return (root, query, builder) -> CollectionUtils.isEmpty(ids) ? builder.disjunction()
                : root.get("id").in(ids);
    }

    public static Specification<DatabaseEntity> nameLike(String name) {
        return (root, query, builder) -> StringUtils.isBlank(name) ? builder.conjunction()
                : builder.like(root.get("name"), "%" + StringUtils.escapeLike(name) + "%");
    }

    public static Specification<DatabaseEntity> projectIdEquals(Long projectId) {
        return (root, query, builder) -> Objects.isNull(projectId) ? builder.conjunction()
                : builder.equal(root.get("projectId"), projectId);
    }

    public static Specification<DatabaseEntity> connectionIdEquals(Long connectionId) {
        return (root, query, builder) -> Objects.isNull(connectionId) ? builder.conjunction()
                : builder.equal(root.get("connectionId"), connectionId);
    }

    public static Specification<DatabaseEntity> organizationIdEquals(Long organizationId) {
        return (root, query, builder) -> Objects.isNull(organizationId) ? builder.conjunction()
                : builder.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<DatabaseEntity> environmentIdEquals(Long environmentId) {
        return (root, query, builder) -> Objects.isNull(environmentId) ? builder.conjunction()
                : builder.equal(root.get("environmentId"), environmentId);
    }

    public static Specification<DatabaseEntity> existedEquals(Boolean existed) {
        return (root, query, builder) -> Objects.isNull(existed) ? builder.conjunction()
                : builder.equal(root.get("existed"), existed);
    }

    public static Specification<DatabaseEntity> projectIdIn(Set<Long> projectIds) {
        return (root, query, builder) -> CollectionUtils.isEmpty(projectIds) ? builder.disjunction()
                : root.get("projectId").in(projectIds);
    }

    public static Specification<DatabaseEntity> projectIdIsNull() {
        return (root, query, builder) -> builder.isNull(root.get("projectId"));
    }

}
