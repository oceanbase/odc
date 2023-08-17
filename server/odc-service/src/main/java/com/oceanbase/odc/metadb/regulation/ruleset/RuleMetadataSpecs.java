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
package com.oceanbase.odc.metadb.regulation.ruleset;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Lists;
import com.oceanbase.odc.service.regulation.ruleset.model.MetadataLabel;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleType;

/**
 * @Author: Lebie
 * @Date: 2023/5/22 19:04
 * @Description: []
 */
public class RuleMetadataSpecs {
    public static Specification<MetadataEntity> typeIn(List<RuleType> types) {
        return (root, query, builder) -> CollectionUtils.isEmpty(types) ? builder.conjunction()
                : root.get("type").in(types);
    }

    public static Specification<MetadataEntity> labels(Map<MetadataLabel, List<String>> labelMap) {
        return (root, query, criteriaBuilder) -> {
            if (Objects.isNull(labelMap) || CollectionUtils.isEmpty(labelMap.keySet())) {
                return criteriaBuilder.conjunction();
            }
            List<Predicate> predicates = Lists.newArrayList();
            query.distinct(true);
            Join<MetadataLabelEntity, MetadataEntity> labels = root.join("labels");
            predicates.add((labels.get("label").in(labelMap.keySet())));
            predicates.add((labels.get("value").in(labelMap.values())));
            return predicates.isEmpty() ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
