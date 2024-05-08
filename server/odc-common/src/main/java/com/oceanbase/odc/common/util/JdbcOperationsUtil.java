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

package com.oceanbase.odc.common.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.sql.DataSource;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Iterables;

/**
 * @author yaobin
 * @date 2023-04-14
 * @since 4.2.0
 */
public class JdbcOperationsUtil {

    public static JdbcOperations getJdbcOperations(Connection connection) {
        return new JdbcTemplate(new SingleConnectionDataSource(connection, false));
    }

    public static TransactionTemplate getTransactionTemplate(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        TransactionTemplate template = new TransactionTemplate();
        template.setTransactionManager(transactionManager);
        template.setIsolationLevel(TransactionTemplate.ISOLATION_DEFAULT);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return template;
    }

    public static <T> List<T> batchCreate(JdbcOperations jdbcOperations, List<T> entities, String sql,
            Map<Integer, Function<T, Object>> valueGetter, BiConsumer<T, Long> idSetter) {
        return jdbcOperations.execute((ConnectionCallback<List<T>>) con -> {
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                try (ResultSet resultSet = ps.getGeneratedKeys()) {
                    int i = 0;
                    while (resultSet.next()) {
                        idSetter.accept(entities.get(i++), getGeneratedId(resultSet));
                    }
                    return entities;
                }
            }
        });
    }

    public static <T> List<T> batchCreate(JdbcOperations jdbcOperations, List<T> entities, String sql,
            Map<Integer, Function<T, Object>> valueGetter, BiConsumer<T, Long> idSetter, int batchSize) {
        Iterable<List<T>> partitions = Iterables.partition(entities, batchSize);
        List<T> result = new ArrayList<>();
        for (List<T> partition : partitions) {
            result.addAll(batchCreate(jdbcOperations, partition, sql, valueGetter, idSetter));
        }
        return result;
    }

    private static Long getGeneratedId(ResultSet resultSet) throws SQLException {
        if (resultSet.getObject("id") != null) {
            return Long.valueOf(resultSet.getObject("id").toString());
        } else if (resultSet.getObject("ID") != null) {
            return Long.valueOf(resultSet.getObject("ID").toString());
        } else if (resultSet.getObject("GENERATED_KEY") != null) {
            return Long.valueOf(resultSet.getObject("GENERATED_KEY").toString());
        }
        return null;
    }

}
