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
package com.oceanbase.odc.core.migrate.resource.model;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.core.migrate.resource.Verifiable;
import com.oceanbase.odc.core.migrate.resource.factory.ValueEncoderFactory.EncodeConfig;
import com.oceanbase.odc.core.migrate.resource.factory.ValueGeneratorFactory.GeneratorConfig;
import com.oceanbase.odc.core.shared.exception.VerifyException;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link TableSpec}
 *
 * @author yh263208
 * @date 2022-04-19 20:51
 * @since ODC_release_3.3.1
 */
@Getter
@Setter
@ToString
public class TableSpec implements Verifiable {
    @NotNull
    @JsonProperty("column_name")
    private String name;
    private boolean ignore = false;
    private Object value;
    @JsonProperty("allow_null")
    private boolean allowNull = false;
    @JsonProperty("default_value")
    private Object defaultValue;
    @JsonProperty("data_type")
    private String dataType;
    @Valid
    @JsonProperty("encode")
    private EncodeConfig encodeConfig;
    @Valid
    @JsonProperty("value_from")
    private ValueFromConfig valueFrom;

    @Override
    public void verify() {
        verifyValue();
        verifyValueFrom();
    }

    private void verifyValue() throws VerifyException {
        if (value != null || defaultValue != null || valueFrom != null || allowNull) {
            return;
        }
        /**
         * value == null && defaultValue == null && allowNull == false
         */
        throw new VerifyException(
                "Field " + name + " can not be null, but the value and the default value are both null");
    }

    private void verifyValueFrom() throws VerifyException {
        if (valueFrom == null) {
            return;
        }
        List<String> nonNullFieldNames = new LinkedList<>();
        try {
            for (Field field : ValueFromConfig.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(valueFrom);
                if (value != null) {
                    nonNullFieldNames.add(field.getName());
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (nonNullFieldNames.isEmpty()) {
            throw new VerifyException("ValueFrom is not available");
        }
        if (nonNullFieldNames.size() != 1) {
            throw new VerifyException("ValueFrom has multi values " + String.join(",", nonNullFieldNames));
        }
    }

    @Getter
    @Setter
    @ToString
    public static class FieldReference {
        @NotNull
        @JsonProperty("field_path")
        private String fieldPath;
        @JsonProperty("ref_file")
        private String refFile;
    }

    @Getter
    @Setter
    @ToString
    public static class DBReference {
        @NotNull
        @JsonProperty("ref_key")
        private String refKey;
        @NotNull
        @JsonProperty("ref_table")
        private String refTable;
        @Valid
        private List<TableSpec> filters;
    }

    @Getter
    @Setter
    @ToString
    public static class ValueFromConfig {
        @Valid
        @JsonProperty("db_ref")
        private DBReference dbRef;
        @Valid
        @JsonProperty("field_ref")
        private FieldReference fieldRef;
        @Valid
        private GeneratorConfig generator;
    }

}
