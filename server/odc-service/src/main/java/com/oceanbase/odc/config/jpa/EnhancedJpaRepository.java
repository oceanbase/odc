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
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.SingularAttribute;
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
import com.oceanbase.odc.common.util.StringUtils;

import cn.hutool.core.lang.func.Func1;
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
    public List<T> batchUpdate(List<T> entities, String sql, Map<Integer, Func1<T, Object>> valueGetter,
            BiConsumer<T, ID> idSetter) {
        Preconditions.checkArgument(entities.stream().allMatch(e -> entityInformation.getId(e) == null),
                "can't create entity, cause not new entities");
        return getJdbcTemplate().execute((ConnectionCallback<List<T>>) con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (T item : entities) {
                for (Entry<Integer, Func1<T, Object>> e : valueGetter.entrySet()) {
                    try {
                        Object call = e.getValue().call(item);
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
    public List<T> batchUpdate(List<T> entities, String sql, List<Func1<T, Object>> valueGetter,
            BiConsumer<T, ID> idSetter) {
        Map<Integer, Func1<T, Object>> valueGetterMap = new HashMap<>();
        IntStream.range(1, valueGetter.size() + 1).forEach(i -> valueGetterMap.put(i, valueGetter.get(i - 1)));
        return batchUpdate(entities, sql, valueGetterMap, idSetter);
    }


    @SneakyThrows
    private ID getGeneratedId(ResultSet resultSet) throws SQLException {
        SingularAttribute<? super T, ?> idAttribute = entityInformation.getIdAttribute();
        Preconditions.checkNotNull(idAttribute, "idAttribute");
        String idName = idAttribute.getName();
        String idColumnName = StringUtils.camelCaseToSnakeCase(idName);
        Field idField = entityInformation.getJavaType().getDeclaredField(idName);
        Column columnAnnotation = idField.getAnnotation(Column.class);
        if (columnAnnotation != null && columnAnnotation.name() != null) {
            idColumnName = columnAnnotation.name();
        }
        Object id = resultSet.getObject(idColumnName);
        Preconditions.checkNotNull(id, "class=" + entityInformation.getJavaType() + ",idColumnName=" + idColumnName);
        return (ID) id;
    }


    public JdbcTemplate getJdbcTemplate() {
        return (JdbcTemplate) namedParameterJdbcTemplate.getJdbcOperations();
    }

    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return namedParameterJdbcTemplate;
    }

}
