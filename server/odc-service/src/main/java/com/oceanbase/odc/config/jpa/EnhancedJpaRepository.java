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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;

import lombok.SneakyThrows;

public class EnhancedJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private EntityManager entityManager;

    private JpaEntityInformation<T, ?> entityInformation;

    public EnhancedJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getDataSource(entityManager));
    }

    public JdbcTemplate getJdbcTemplate() {
        return (JdbcTemplate) namedParameterJdbcTemplate.getJdbcOperations();
    }

    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return namedParameterJdbcTemplate;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @SneakyThrows
    @Transactional
    public List<T> batchCreate(List<T> entities, String sql, Map<Integer, Function<T, Object>> valueGetter,
            BiConsumer<T, Long> idSetter) {
        Preconditions.checkArgument(entities.stream().allMatch(e -> entityInformation.getId(e) == null),
                "can't create entity, cause not new entities");
        return JdbcOperationsUtil.batchCreate(getJdbcTemplate(), entities, sql, valueGetter, idSetter);
    }

    @SneakyThrows
    @Transactional
    public List<T> batchCreate(List<T> entities, String sql, List<Function<T, Object>> valueGetter,
            BiConsumer<T, Long> idSetter) {
        Map<Integer, Function<T, Object>> valueGetterMap = new HashMap<>();
        IntStream.range(1, valueGetter.size() + 1).forEach(i -> valueGetterMap.put(i, valueGetter.get(i - 1)));
        return batchCreate(entities, sql, valueGetterMap, idSetter);
    }

    @Override
    protected <S extends T> TypedQuery<Long> getCountQuery(Specification<S> spec, Class<S> domainClass) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);

        Root<S> root = applySpecificationToCriteria(spec, domainClass, query);

        /**
         * if group by, we calculate the count of the group instead of the sum of all group items<br/>
         * this is the only difference with SimpleJpaRepository#getCountQuery
         */
        if (query.isDistinct() || !query.getGroupList().isEmpty()) {
            query.select(builder.countDistinct(root));
        } else {
            query.select(builder.count(root));
        }

        // Remove all Orders the Specifications might have applied
        query.orderBy(Collections.emptyList());

        return entityManager.createQuery(query);
    }

    private DataSource getDataSource(EntityManager entityManager) {
        SessionFactoryImpl sf = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImpl.class);
        return ((DatasourceConnectionProviderImpl) sf.getServiceRegistry().getService(ConnectionProvider.class))
                .getDataSource();
    }

    private Long getGeneratedId(ResultSet resultSet) throws SQLException {
        if (resultSet.getObject("id") != null) {
            return Long.valueOf(resultSet.getObject("id").toString());
        } else if (resultSet.getObject("ID") != null) {
            return Long.valueOf(resultSet.getObject("ID").toString());
        } else if (resultSet.getObject("GENERATED_KEY") != null) {
            return Long.valueOf(resultSet.getObject("GENERATED_KEY").toString());
        }
        return null;
    }


    private <S, U extends T> Root<U> applySpecificationToCriteria(@Nullable Specification<U> spec, Class<U> domainClass,
            CriteriaQuery<S> query) {

        Assert.notNull(domainClass, "Domain class must not be null!");
        Assert.notNull(query, "CriteriaQuery must not be null!");

        Root<U> root = query.from(domainClass);

        if (spec == null) {
            return root;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        Predicate predicate = spec.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }

        return root;
    }

}
