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
package com.oceanbase.odc.metadb.script;

import java.util.List;
import java.util.Objects;

import javax.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Lists;
import com.oceanbase.odc.service.script.model.QueryScriptMetaParams;

/**
 * @Author: Lebie
 * @Date: 2022/3/22 下午9:29
 * @Description: []
 */
public class ScriptMetaSpecs {
    public static Specification<ScriptMetaEntity> of(QueryScriptMetaParams params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = Lists.newArrayList();
            if (Objects.nonNull(params.getBucketName())) {
                predicates.add(criteriaBuilder.equal(root.get("bucketName"), params.getBucketName()));
            }
            if (Objects.nonNull(params.getCreatorId())) {
                predicates.add(criteriaBuilder.equal(root.get("creatorId"), params.getCreatorId()));
            }
            if (CollectionUtils.isNotEmpty(params.getObjectIds())) {
                predicates.add(root.get("objectId").in(params.getObjectIds()));
            }
            return predicates.isEmpty() ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
