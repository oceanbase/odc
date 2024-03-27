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

import static org.springframework.data.jpa.repository.query.QueryUtils.toOrders;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort.JpaOrder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.common.util.EmptyValues;

import lombok.NonNull;

/**
 * @author yizhou.xw
 * @version : ConnectionSpecs.java, v 0.1 2021-07-22 20:10
 */
public class ConnectionSpecs {

    public static Specification<ConnectionEntity> idIn(Collection<Long> ids) {
        return in("id", ids, Long.class);
    }

    public static Specification<ConnectionEntity> sort(@NonNull Sort sort) {
        return (root, query, builder) -> {
            if (sort.isUnsorted() || !(builder instanceof CriteriaBuilderImpl)) {
                return builder.conjunction();
            }
            CriteriaBuilderImpl impl = (CriteriaBuilderImpl) builder;
            query.orderBy(sort.stream().map(order -> {
                if (order instanceof JpaOrder) {
                    Class<ConnectionEntity> clazz = ConnectionEntity.class;
                    JpaOrder jpaOrder = (JpaOrder) order;
                    Expression<ConnectionEntity> expr = new JpaOrderExpression<>(impl, clazz, jpaOrder);
                    return order.isAscending() ? builder.asc(expr) : builder.desc(expr);
                }
                return toOrders(Sort.by(order), root, builder).get(0);
            }).collect(Collectors.toList()));
            return builder.conjunction();
        };
    }

    public static Specification<ConnectionEntity> clusterNameIn(List<String> clusterNames) {
        return in("clusterName", clusterNames, String.class);
    }

    public static Specification<ConnectionEntity> tenantNameIn(List<String> tenantNames) {
        return in("tenantName", tenantNames, String.class);
    }

    public static Specification<ConnectionEntity> typeIn(List<ConnectType> types) {
        return in("type", types, ConnectType.class);
    }

    public static Specification<ConnectionEntity> labelRelatedConnIdIn(Set<Long> connIds) {
        if (connIds == null) {
            // 为 null 代表用户没有使用 labelId 进行筛选
            return (root, query, builder) -> builder.conjunction();
        } else if (connIds.isEmpty()) {
            // 为 empty 代表目标 labelId 对应的 conn 为空
            return (root, query, builder) -> builder.disjunction();
        }
        return idIn(connIds);
    }

    public static Specification<ConnectionEntity> organizationIdEqual(Long organizationId) {
        return columnEqual("organizationId", organizationId);
    }

    public static Specification<ConnectionEntity> visibleScopeEqual(ConnectionVisibleScope visibleScope) {
        return columnEqual("visibleScope", visibleScope);
    }

    public static Specification<ConnectionEntity> not() {
        return (root, query, builder) -> builder.disjunction();
    }

    public static Specification<ConnectionEntity> enabledEqual(Boolean enabled) {
        return columnEqual("enabled", enabled);
    }

    public static Specification<ConnectionEntity> userIdEqual(Long userId) {
        if (Objects.isNull(userId)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return columnEqual("visibleScope", ConnectionVisibleScope.PRIVATE).and(columnEqual("ownerId", userId));
    }

    public static Specification<ConnectionEntity> usernameEqual(String username) {
        return columnEqual("username", username);
    }

    public static Specification<ConnectionEntity> dialectTypeIn(List<DialectType> dialectTypes) {
        return in("dialectType", dialectTypes, DialectType.class);
    }

    public static Specification<ConnectionEntity> nameLike(String name) {
        return (root, query, builder) -> StringUtils.isBlank(name) ? builder.conjunction()
                : builder.like(root.get("name"), "%" + StringUtils.escapeLike(name) + "%");
    }

    public static Specification<ConnectionEntity> idLike(String id) {
        return (root, query, builder) -> StringUtils.isBlank(id) ? builder.conjunction()
                : builder.like(root.get("id").as(String.class), "%" + StringUtils.escapeLike(id) + "%");
    }

    public static Specification<ConnectionEntity> hostLike(String host) {
        return (root, query, builder) -> StringUtils.isBlank(host) ? builder.conjunction()
                : builder.like(root.get("host").as(String.class), "%" + StringUtils.escapeLike(host) + "%");
    }

    public static Specification<ConnectionEntity> clusterNameLike(String cluster) {
        return (root, query, builder) -> StringUtils.isBlank(cluster) ? builder.conjunction()
                : builder.like(root.get("clusterName").as(String.class), "%" + StringUtils.escapeLike(cluster) + "%");
    }

    public static Specification<ConnectionEntity> clusterNamesLike(Collection<String> clusters) {
        if (CollectionUtils.isEmpty(clusters)) {
            return (root, query, builder) -> builder.conjunction();
        }
        String field = "clusterName";
        return clusters.stream().filter(StringUtils::isNotBlank).map(s -> {
            if (EmptyValues.matches(s)) {
                return columnEqualsNull(field).or(columnEqual(field, EmptyValues.STRING_VALUE));
            }
            return ConnectionSpecs.clusterNameLike(s);
        }).reduce(Specification::or).orElseGet(() -> (root, query, builder) -> builder.conjunction());
    }

    public static Specification<ConnectionEntity> tenantNamesLike(Collection<String> tenants) {
        if (CollectionUtils.isEmpty(tenants)) {
            return (root, query, builder) -> builder.conjunction();
        }
        String field = "tenantName";
        return tenants.stream().filter(StringUtils::isNotBlank).map(s -> {
            if (EmptyValues.matches(s)) {
                return columnEqualsNull(field).or(columnEqual(field, EmptyValues.STRING_VALUE));
            }
            return ConnectionSpecs.tenantNameLike(s);
        }).reduce(Specification::or).orElseGet(() -> (root, query, builder) -> builder.conjunction());
    }

    public static Specification<ConnectionEntity> tenantNameLike(String tenant) {
        return (root, query, builder) -> StringUtils.isBlank(tenant) ? builder.conjunction()
                : builder.like(root.get("tenantName").as(String.class), "%" + StringUtils.escapeLike(tenant) + "%");
    }

    public static Specification<ConnectionEntity> portLike(String port) {
        return (root, query, builder) -> StringUtils.isBlank(port) ? builder.conjunction()
                : builder.like(root.get("port").as(String.class), "%" + StringUtils.escapeLike(port) + "%");
    }

    public static Specification<ConnectionEntity> isNotTemp() {
        return (root, query, builder) -> builder.equal(root.get("temp"), false);
    }

    private static <T> Specification<ConnectionEntity> in(String column, Collection<T> values, Class<T> classType) {
        if (CollectionUtils.isEmpty(values)) {
            return conjunction();
        }
        List<T> notEmptyValues = values.stream()
                .filter(t -> !EmptyValues.matches(t))
                .collect(Collectors.toList());
        boolean hasEmptyValue = values.size() - notEmptyValues.size() > 0;
        if (!hasEmptyValue) {
            return columnIn(column, values);
        }
        return (root, query, builder) -> {
            List<Predicate> predicates = new LinkedList<>();
            // for mysql, in(empty collection) mapping to ' IN (null)'
            // for h2database, in(empty collection) mapping to 'IN ()', which is illegal
            // we filter empty collection scenario for support both database type
            if (!CollectionUtils.isEmpty(notEmptyValues)) {
                predicates.add(root.get(column).in(notEmptyValues));
            }
            if (String.class.equals(classType)) {
                predicates.add(builder.equal(root.get(column), EmptyValues.STRING_VALUE));
            }
            predicates.add(builder.isNull(root.get(column)));
            return builder.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<ConnectionEntity> conjunction() {
        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<ConnectionEntity> columnIn(String column, Collection<?> values) {
        PreConditions.notEmpty(values, "values");
        return (root, query, builder) -> root.get(column).in(values);
    }

    private static Specification<ConnectionEntity> columnEqualsNull(String column) {
        return (root, query, builder) -> builder.isNull(root.get(column));
    }

    private static Specification<ConnectionEntity> columnEqual(String column, Object value) {
        return (root, query, builder) -> {
            if (Objects.isNull(value)) {
                return builder.conjunction();
            } else {
                return builder.equal(root.get(column), value);
            }
        };
    }
}
