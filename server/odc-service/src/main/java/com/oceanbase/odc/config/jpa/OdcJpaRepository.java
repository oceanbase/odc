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
package com.oceanbase.odc.config.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Iterables;

public interface OdcJpaRepository<T, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    JdbcTemplate getJdbcTemplate();

    NamedParameterJdbcTemplate getNamedParameterJdbcTemplate();

    EntityManager getEntityManager();

    /**
     *
     * @param entities
     * @param sql
     * @param valueGetter
     * @param idSetter
     * @return entities and assign generated id;
     */
    List<T> batchCreate(List<T> entities, String sql, Map<Integer, Function<T, Object>> valueGetter,
            BiConsumer<T, Long> idSetter);

    List<T> batchCreate(List<T> entities, String sql, List<Function<T, Object>> valueGetter,
            BiConsumer<T, Long> idSetter);

    List<T> batchCreate(List<T> entities, String sql, Map<Integer, Function<T, Object>> valueGetter,
            BiConsumer<T, Long> idSetter, int batchSize);

    List<T> batchCreate(List<T> entities, String sql, List<Function<T, Object>> valueGetter,
            BiConsumer<T, Long> idSetter, int batchSize);


    class ValueGetterBuilder<T> {

        private final List<Function<T, Object>> valueGetter = new ArrayList<>();

        public ValueGetterBuilder<T> add(Function<T, Object> getter) {
            valueGetter.add(getter);
            return this;
        }

        public List<Function<T, Object>> build() {
            return valueGetter;
        }
    }

    default ValueGetterBuilder<T> valueGetterBuilder() {
        return new ValueGetterBuilder<>();
    }

    default <Y, E> List<E> partitionFind(Collection<Y> ids, Function<List<Y>, List<E>> func) {
        return partitionFind(ids, 100, func);
    }

    default <Y, E> List<E> partitionFind(Collection<Y> ids, int partitionSize, Function<List<Y>, List<E>> func) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<E> result = new ArrayList<>();
        for (List<Y> part : Iterables.partition(new HashSet<>(ids), partitionSize)) {
            if (part.size() < partitionSize) {
                List<Y> padding = new ArrayList<>(part);
                for (int i = 0; i < partitionSize - part.size(); i++) {
                    padding.add(part.get(0));
                }
                part = padding;
            }
            result.addAll(func.apply(part));
        }
        return result;
    }

    static <T, Y extends Comparable<? super Y>> Specification<T> isNull(SingularAttribute<T, Y> attr) {
        return (root, query, cb) -> cb.isNull(root.get(attr));
    }

    static <T, Y extends Comparable<? super Y>> Specification<T> isNotNull(SingularAttribute<T, Y> attr) {
        return (root, query, cb) -> cb.isNotNull(root.get(attr));
    }

    static <T> Specification<T> eq(SingularAttribute<T, ?> attr, Object value) {
        return isNullOrEmpty(value) ? null : (root, query, cb) -> cb.equal(root.get(attr), value);
    }

    static <T> Specification<T> notEq(SingularAttribute<T, ?> attr, Object value) {
        return isNullOrEmpty(value) ? null : (root, query, cb) -> cb.notEqual(root.get(attr), value);
    }

    static <T> Specification<T> startsWith(SingularAttribute<T, ?> attr, Object value) {
        return isNullOrEmpty(value) ? null
                : (root, query, cb) -> cb.like(root.<String>get(attr.getName()), value.toString() + "%");
    }

    static <T> Specification<T> in(SingularAttribute<T, ?> attr, Collection<?> values) {
        if (isNullOrEmpty(values)) {
            return null;
        }
        return (root, query, cb) -> {
            Path<?> expression = root.get(attr);
            return cb.isTrue(expression.in(values));
        };
    }

    static <T> Specification<T> like(SingularAttribute<T, String> attr, String value) {
        if (isNullOrEmpty(value)) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get(attr), "%" + value + "%");
    }

    static <T> Specification<T> notIn(SingularAttribute<T, ?> attr, Collection<?> values) {
        if (isNullOrEmpty(values)) {
            return null;
        }
        return (root, query, cb) -> {
            Path<?> expression = root.get(attr);
            return cb.isTrue(expression.in(values).not());
        };
    }

    static <T, Y extends Comparable<? super Y>> Specification<T> lt(SingularAttribute<T, Y> attr, Y value) {
        return isNullOrEmpty(value) ? null : (root, query, cb) -> cb.lessThan(root.get(attr), value);
    }

    static <T, Y extends Comparable<? super Y>> Specification<T> lte(SingularAttribute<T, Y> attr, Y value) {
        return isNullOrEmpty(value) ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get(attr), value);
    }

    static <T, Y extends Comparable<? super Y>> Specification<T> gt(SingularAttribute<T, Y> attr, Y value) {
        return isNullOrEmpty(value) ? null : (root, query, cb) -> cb.greaterThan(root.get(attr), value);
    }

    static <T, Y extends Comparable<? super Y>> Specification<T> gte(SingularAttribute<T, Y> attr, Y value) {
        return isNullOrEmpty(value) ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(attr), value);
    }


    static <T, Y extends Comparable<? super Y>> Specification<T> between(SingularAttribute<T, Y> attr, Y from, Y to) {
        if (from == null) {
            return lte(attr, to);
        }
        if (to == null) {
            return gte(attr, from);
        }
        return (root, query, cb) -> cb.between(root.get(attr), from, to);
    }

    static <T, J> Specification<T> eq(SingularAttribute<T, J> joinAttr, SingularAttribute<J, ?> attr, Object value) {
        return isNullOrEmpty(value) ? null
                : (root, query, cb) -> cb.equal(root.join(joinAttr, JoinType.LEFT).get(attr), value);
    }

    static <T, J> Specification<T> lt(SingularAttribute<T, J> joinAttr,
            SingularAttribute<J, ? extends Number> attr, Number value) {
        return isNullOrEmpty(value) ? null
                : (root, query, cb) -> cb.lt(root.join(joinAttr, JoinType.LEFT).get(attr), value);
    }

    static <T, Y extends Comparable<? super Y>> Specification<T> le(SingularAttribute<T, Y> attr, Y value) {
        return isNullOrEmpty(value) ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get(attr), value);
    }

    static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    static boolean isNullOrEmpty(Object obj) {
        return obj == null || obj instanceof String && ((String) obj).isEmpty();
    }

}
