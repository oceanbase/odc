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
package com.oceanbase.odc.metadb.objectstorage;

import java.util.List;
import java.util.Objects;

import javax.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Lists;
import com.oceanbase.odc.service.objectstorage.model.QueryObjectMetadataParam;

/**
 * @Author: Lebie
 * @Date: 2022/3/16 下午4:59
 * @Description: []
 */
public class ObjectMetadataSpecs {
    public static Specification<ObjectMetadataEntity> of(QueryObjectMetadataParam params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(params.getObjectIds())) {
                predicates.add(root.get("objectId").in(params.getObjectIds()));
            }
            if (Objects.nonNull(params.getBucketName())) {
                predicates.add(criteriaBuilder.equal(root.get("bucketName"), params.getBucketName()));
            }
            if (Objects.nonNull(params.getObjectName())) {
                predicates.add(criteriaBuilder.equal(root.get("objectName"), params.getObjectName()));
            }
            return predicates.isEmpty() ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
