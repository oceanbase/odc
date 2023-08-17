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
package com.oceanbase.odc.metadb.audit;

import java.util.List;
import java.util.Objects;

import javax.persistence.criteria.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.audit.model.QueryAuditEventMetaParams;
import com.oceanbase.odc.service.audit.model.QueryAuditEventParams;
import com.oceanbase.odc.service.common.util.SqlUtils;

/**
 * @Author: Lebie
 * @Date: 2022/1/19 下午4:42
 * @Description: []
 */
public class AuditSpecs {
    public static Specification<AuditEventEntity> of(QueryAuditEventParams params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(params.getTypes())) {
                predicates.add(root.get("type").in(params.getTypes()));
            }

            if (CollectionUtils.isNotEmpty(params.getActions())) {
                predicates.add(root.get("action").in(params.getActions()));
            }

            if (CollectionUtils.isNotEmpty(params.getUserIds())) {
                predicates.add(root.get("userId").in(params.getUserIds()));
            }

            if (CollectionUtils.isNotEmpty(params.getConnectionIds())) {
                predicates.add(root.get("connectionId").in(params.getConnectionIds()));
            }

            if (StringUtils.isNotEmpty(params.getFuzzyClientIpAddress())) {
                predicates.add(criteriaBuilder.like(root.get("clientIpAddress"),
                        SqlUtils.anyLike(params.getFuzzyClientIpAddress())));
            }
            if (StringUtils.isNotEmpty(params.getFuzzyConnectionName())) {
                predicates.add(criteriaBuilder.like(root.get("connectionName"),
                        SqlUtils.anyLike(params.getFuzzyConnectionName())));
            }

            if (CollectionUtils.isNotEmpty(params.getResults())) {
                predicates.add(root.get("result").in(params.getResults()));
            }

            if (StringUtils.isNotEmpty(params.getFuzzyUsername())) {
                predicates.add(criteriaBuilder.like(root.get("username"),
                        SqlUtils.anyLike(params.getFuzzyUsername())));
            }

            if (Objects.nonNull(params.getOrganizationId())) {
                predicates.add(criteriaBuilder.equal(root.get("organizationId"), params.getOrganizationId()));
            }

            if (Objects.nonNull(params.getStartTime())) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), params.getStartTime()));
            }

            if (Objects.nonNull(params.getEndTime())) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), params.getEndTime()));
            }

            return predicates.isEmpty() ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<AuditEventMetaEntity> of(QueryAuditEventMetaParams params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(params.getActions())) {
                predicates.add(root.get("action").in(params.getActions()));
            }
            if (CollectionUtils.isNotEmpty(params.getTypes())) {
                predicates.add(root.get("type").in(params.getTypes()));
            }
            if (Objects.nonNull(params.getEnabled())) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), params.getEnabled()));
            }
            if (Objects.nonNull(params.getInConnection())) {
                predicates.add(criteriaBuilder.equal(root.get("inConnection"), params.getInConnection()));
            }
            return predicates.isEmpty() ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
