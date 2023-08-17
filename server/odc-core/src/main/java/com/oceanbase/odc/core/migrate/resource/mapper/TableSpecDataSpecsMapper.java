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
package com.oceanbase.odc.core.migrate.resource.mapper;

import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.common.util.ListUtils;
import com.oceanbase.odc.common.util.MapperUtils;
import com.oceanbase.odc.core.migrate.resource.ResourceManager;
import com.oceanbase.odc.core.migrate.resource.factory.ValueEncoderFactory;
import com.oceanbase.odc.core.migrate.resource.factory.ValueEncoderFactory.EncodeConfig;
import com.oceanbase.odc.core.migrate.resource.factory.ValueGeneratorFactory;
import com.oceanbase.odc.core.migrate.resource.factory.ValueGeneratorFactory.GeneratorConfig;
import com.oceanbase.odc.core.migrate.resource.model.DataSpec;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec.DBReference;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec.FieldReference;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec.ValueFromConfig;
import com.oceanbase.odc.core.migrate.resource.util.PrefixPathMatcher;
import com.oceanbase.odc.core.migrate.resource.value.ValueEncoder;
import com.oceanbase.odc.core.migrate.resource.value.ValueGenerator;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link TableSpecDataSpecsMapper}
 *
 * @author yh263208
 * @date 2022-04-20 11:15
 * @since ODC_release_3.3.1
 */
@Slf4j
public class TableSpecDataSpecsMapper implements EntityMapper<TableSpec, List<DataSpec>> {

    private final ValueEncoderFactory encoderFactory;
    private final ValueGeneratorFactory generatorFactory;
    private final ResourceManager manager;
    private final DataSource dataSource;
    private final ResourceSpec defaultEntity;
    private static final Pattern PLACE_HOLDER_PATTERN = Pattern.compile("^\\$\\{[0-9a-zA-Z_\\-]+\\}$");

    public TableSpecDataSpecsMapper(@NonNull ResourceSpec defaultEntity,
            @NonNull ValueEncoderFactory encoderFactory, @NonNull ValueGeneratorFactory generatorFactory,
            @NonNull ResourceManager manager, @NonNull DataSource dataSource) {
        this.encoderFactory = encoderFactory;
        this.manager = manager;
        this.defaultEntity = defaultEntity;
        this.dataSource = dataSource;
        this.generatorFactory = generatorFactory;
    }

    /**
     * from {@link TableSpec} to list of {@link DataSpec}
     *
     * @param entity entity model
     * @return list of {@link DataSpec}
     */
    @Override
    public List<DataSpec> entityToModel(@NonNull TableSpec entity) {
        entity.verify();
        Object value = getValue(entity.getValue());
        if (value != null) {
            return Collections.singletonList(getDataSpec(entity, value));
        }
        ValueFromConfig valueFromConfig = entity.getValueFrom();
        if (valueFromConfig != null) {
            GeneratorConfig config = valueFromConfig.getGenerator();
            if (config != null) {
                ValueGenerator<?> generator = generatorFactory.generate(config);
                value = getValue(generator.generate());
                if (value != null) {
                    return Collections.singletonList(getDataSpec(entity, value));
                }
            }
            FieldReference fieldRef = valueFromConfig.getFieldRef();
            if (fieldRef != null) {
                List<Object> values = getValueFromRef(fieldRef);
                if (values != null) {
                    return values.stream().map(v -> getDataSpec(entity, getValue(v))).collect(Collectors.toList());
                }
            }
            DBReference dbRef = valueFromConfig.getDbRef();
            if (dbRef != null) {
                return getValueFromRef(dbRef).stream().map(v -> getDataSpec(entity, v)).collect(Collectors.toList());
            }
        }
        if (entity.getDefaultValue() != null) {
            return Collections.singletonList(getDataSpec(entity, entity.getDefaultValue()));
        }
        if (entity.isAllowNull()) {
            return Collections.singletonList(getDataSpec(entity, null));
        }
        throw new NullPointerException("Data can not be null, spec: " + entity);
    }

    private DataSpec getDataSpec(@NonNull TableSpec entity, Object value) {
        if (value == null) {
            return new DataSpec(entity, value);
        }
        EncodeConfig encodeConfig = entity.getEncodeConfig();
        if (encodeConfig != null) {
            ValueEncoder encoder = encoderFactory.generate(encodeConfig);
            return new DataSpec(entity, encoder.encode(value.toString()));
        }
        String dataType = entity.getDataType();
        if (dataType == null) {
            return new DataSpec(entity, value);
        }
        try {
            Class<?> clazz = Class.forName(dataType);
            Constructor<?> constructor = clazz.getConstructor(String.class);
            return new DataSpec(entity, constructor.newInstance(value.toString()));
        } catch (Exception e) {
            log.warn("Failed to get class, dataType={}", dataType, e);
            throw new RuntimeException(e);
        }
    }

    private List<Object> getFieldReferValues(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List<Object> result = ((List<?>) value).stream().filter(Objects::nonNull).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(result)) {
                return result;
            }
            return null;
        }
        return Collections.singletonList(value);
    }

    private List<Object> getValueFromRef(@NonNull DBReference dbReference) {
        List<List<DataSpec>> dataSpecs = null;
        List<TableSpec> filters = dbReference.getFilters();
        if (CollectionUtils.isNotEmpty(filters)) {
            dataSpecs = filters.stream().map(this::entityToModel).collect(Collectors.toList());
            dataSpecs = ListUtils.cartesianProduct(dataSpecs);
        }
        List<Object> parameters = new LinkedList<>();
        String sql = generateSelectSql(dbReference.getRefTable(), dbReference.getRefKey(), dataSpecs, parameters);
        return query(sql, parameters);
    }

    private List<Object> getValueFromRef(@NonNull FieldReference fieldRef) {
        String refPath = fieldRef.getRefFile();
        String fieldPath = fieldRef.getFieldPath();
        if (refPath == null) {
            return getFieldReferValues(MapperUtils.get(defaultEntity, Object.class, new PrefixPathMatcher(fieldPath)));
        }
        List<ResourceSpec> entities = manager.findBySuffix(refPath);
        if (entities.size() != 1) {
            throw new IllegalStateException("Resource not available by suffix " + refPath);
        }
        return getFieldReferValues(MapperUtils.get(entities.get(0), Object.class, new PrefixPathMatcher(fieldPath)));
    }

    private List<Object> query(@NonNull String sql, @NonNull List<Object> parameters) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        if (log.isDebugEnabled()) {
            log.debug("Sql query, sql={}, params={}", sql, parameters);
        }
        return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql);
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            return statement;
        }, (resultSet, i) -> resultSet.getObject(1));
    }

    private Object getValue(Object value) {
        if (value == null || PLACE_HOLDER_PATTERN.matcher(value.toString()).matches()) {
            return null;
        }
        return value;
    }

    private String generateSelectSql(@NonNull String table, @NonNull String column, List<List<DataSpec>> whereCauses,
            @NonNull List<Object> parameters) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT `").append(column).append("` FROM ")
                .append("`").append(table).append("`");
        if (whereCauses == null || whereCauses.isEmpty()) {
            return sqlBuilder.toString();
        }
        List<String> conditions =
                whereCauses.stream().filter(s -> s != null && !s.isEmpty()).map(s -> s.stream().map(subData -> {
                    parameters.add(subData.getValue());
                    return "`" + subData.getName() + "`=?";
                }).collect(Collectors.joining(" AND "))).map(s -> "( " + s + " )").collect(Collectors.toList());
        if (conditions.isEmpty()) {
            return sqlBuilder.toString();
        }
        return sqlBuilder.append(" WHERE ").append(String.join(" OR ", conditions)).toString();
    }

}
