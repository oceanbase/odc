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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.base.Preconditions;

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

    private DataSource getDataSource(EntityManager entityManager) {
        SessionFactoryImpl sf = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImpl.class);
        return ((DatasourceConnectionProviderImpl) sf.getServiceRegistry().getService(ConnectionProvider.class))
                .getDataSource();
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @SneakyThrows
    public List<T> batchCreate(List<T> entities, String sql, Map<Integer, Function<T, Object>> valueGetter,
            BiConsumer<T, Long> idSetter) {
        Preconditions.checkArgument(entities.stream().allMatch(e -> entityInformation.getId(e) == null),
                "can't create entity, cause not new entities");
        return getJdbcTemplate().execute((ConnectionCallback<List<T>>) con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (T item : entities) {
                for (Entry<Integer, Function<T, Object>> e : valueGetter.entrySet()) {
                    try {
                        Object call = e.getValue().apply(item);
                        ps.setObject(e.getKey(), call);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet resultSet = ps.getGeneratedKeys();
            int i = 0;
            while (resultSet.next()) {
                idSetter.accept(entities.get(i++), getGeneratedId(resultSet));
            }
            return entities;
        });
    }

    @SneakyThrows
    public List<T> batchCreate(List<T> entities, String sql, List<Function<T, Object>> valueGetter,
            BiConsumer<T, Long> idSetter) {
        Map<Integer, Function<T, Object>> valueGetterMap = new HashMap<>();
        IntStream.range(1, valueGetter.size() + 1).forEach(i -> valueGetterMap.put(i, valueGetter.get(i - 1)));
        return batchCreate(entities, sql, valueGetterMap, idSetter);
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

    public JdbcTemplate getJdbcTemplate() {
        return (JdbcTemplate) namedParameterJdbcTemplate.getJdbcOperations();
    }

    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return namedParameterJdbcTemplate;
    }

}
