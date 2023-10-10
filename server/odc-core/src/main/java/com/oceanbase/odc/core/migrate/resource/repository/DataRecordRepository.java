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
package com.oceanbase.odc.core.migrate.resource.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.DataSpec;
import com.oceanbase.odc.core.shared.Verify;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository for {@link com.oceanbase.odc.core.migrate.resource.model.DataRecord}
 *
 * @author yh263208
 * @date 2022-04-22 13:52
 * @since ODC_release_3.3.1
 */
@Slf4j
public class DataRecordRepository {

    private final DataSource dataSource;

    public DataRecordRepository(@NonNull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataRecord save(@NonNull DataRecord record) {
        List<DataSpec> savedSpecs = record.getData().stream().filter(dataSpec -> !dataSpec.isIgnore())
                .collect(Collectors.toList());
        String insertSql = generateInsertSql(record.getTableName(), savedSpecs);
        final Long id = insert(insertSql, savedSpecs);
        List<DataSpec> specs = new LinkedList<>();
        record.getData().forEach(src -> {
            Object value = src.getValue();
            if ("id".equals(src.getName())) {
                value = id;
            }
            specs.add(DataSpec.copyFrom(src, value));
        });
        return DataRecord.copyFrom(record, specs);
    }

    public List<DataRecord> find(@NonNull DataRecord record) {
        String selectSql = generateSelectSql(record.getTableName(), record.getKeys(), record.getUniqueKeyData());
        return query(selectSql, record.getUniqueKeyData(), (resultSet, i) -> DataRecord.copyFrom(resultSet, record));
    }

    public boolean exists(@NonNull DataRecord record) {
        List<DataSpec> conditions = record.getUniqueKeyData();
        String sql = generateSelectSql(record.getTableName(), "COUNT(1)", conditions);
        List<Long> result = query(sql, conditions, (resultSet, i) -> resultSet.getLong(1));
        Verify.singleton(result, "Count(1) has to be singleton");
        return result.get(0) >= 1;
    }

    private Long insert(@NonNull String sql, List<DataSpec> dataSpecs) {
        if (log.isDebugEnabled()) {
            log.debug("Sql update, sql={}, params={}", sql,
                    dataSpecs == null ? null : dataSpecs.stream().map(DataSpec::getValue).collect(Collectors.toList()));
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int affectRows = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (dataSpecs == null) {
                return statement;
            }
            for (int i = 0; i < dataSpecs.size(); i++) {
                statement.setObject(i + 1, dataSpecs.get(i).getValue());
            }
            return statement;
        }, keyHolder);
        if (log.isDebugEnabled()) {
            log.debug("Record has been saved, affectRows={}", affectRows);
        }
        if (affectRows != 1) {
            throw new IllegalStateException("Saved nothing, record " + affectRows);
        }
        Map<String, Object> keyMap = keyHolder.getKeys();
        if (keyMap == null) {
            return null;
        }
        if (keyMap.size() == 1) {
            return keyHolder.getKeyAs(Long.class);
        }
        if (keyMap.get("id") != null) {
            return Long.valueOf(keyMap.get("id").toString());
        } else if (keyMap.get("ID") != null) {
            return Long.valueOf(keyMap.get("ID").toString());
        } else if (keyMap.get("GENERATED_KEY") != null) {
            return Long.valueOf(keyMap.get("GENERATED_KEY").toString());
        }
        return null;
    }

    private <T> List<T> query(@NonNull String sql, List<DataSpec> params, @NonNull RowMapper<T> mapper) {
        if (log.isDebugEnabled()) {
            log.debug("Sql query, sql={}, params={}", sql,
                    params == null ? null : params.stream().map(DataSpec::getValue).collect(Collectors.toList()));
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql);
            if (params == null) {
                return statement;
            }
            int parameterIndex = 1;
            for (DataSpec dataSpec : params) {
                if (Objects.nonNull(dataSpec.getValue())) {
                    statement.setObject(parameterIndex++, dataSpec.getValue());
                }
            }
            return statement;
        }, mapper);
    }

    private String generateSelectSql(@NonNull String tableName, @NonNull Set<String> columns,
            List<DataSpec> conditions) {
        return generateSelectSql(tableName, String.join(",", columns.stream().map(s -> "`" + s + "`")
                .collect(Collectors.toSet())), conditions);
    }

    private String generateSelectSql(@NonNull String tableName, @NonNull String columns, List<DataSpec> conditions) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT ").append(columns)
                .append(" FROM `").append(tableName).append("`");
        if (conditions != null && !conditions.isEmpty()) {
            List<String> whereCauses = conditions.stream().map(dataSpec -> {
                if (dataSpec.getValue() != null) {
                    return "`" + dataSpec.getName() + "`=?";
                }
                return "`" + dataSpec.getName() + "` is null";
            }).collect(Collectors.toList());
            sqlBuilder.append(" WHERE ").append(String.join(" AND ", whereCauses));
        }
        return sqlBuilder.toString();
    }

    private String generateInsertSql(@NonNull String tableName, @NonNull List<DataSpec> dataSpecs) {
        if (CollectionUtils.isEmpty(dataSpecs)) {
            throw new IllegalArgumentException("Insert data can not be empty");
        }
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO `").append(tableName).append("` (");
        List<String> columns = dataSpecs.stream().map(dataSpec -> "`" + dataSpec.getName() + "`")
                .collect(Collectors.toList());
        sqlBuilder.append(String.join(",", columns));
        String[] value = new String[columns.size()];
        Arrays.fill(value, "?");
        return sqlBuilder.append(") VALUES (").append(String.join(",", value)).append(")").toString();
    }

}
