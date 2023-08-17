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

import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2023/4/20 21:26
 * @Description: []
 */
public class ProjectSpecs {
    public static Specification<ProjectEntity> nameLike(String name) {
        return (root, query, builder) -> StringUtils.isBlank(name) ? builder.conjunction()
                : builder.like(root.get("name"), "%" + StringUtils.escapeLike(name) + "%");
    }

    public static Specification<ProjectEntity> archivedEqual(Boolean archived) {
        return (root, query, builder) -> Objects.isNull(archived) ? builder.conjunction()
                : builder.equal(root.get("archived"), archived);
    }

    public static Specification<ProjectEntity> organizationIdEqual(Long organizationId) {
        return (root, query, builder) -> Objects.isNull(organizationId) ? builder.conjunction()
                : builder.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<ProjectEntity> builtInEqual(Boolean builtIn) {
        return (root, query, builder) -> Objects.isNull(builtIn) ? builder.conjunction()
                : builder.equal(root.get("builtin"), builtIn);
    }

    public static Specification<ProjectEntity> idIn(List<Long> ids) {
        return (root, query, builder) -> CollectionUtils.isEmpty(ids) ? builder.disjunction()
                : root.get("id").in(ids);
    }
}
