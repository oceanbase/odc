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
package com.oceanbase.odc.common.jpa;

import java.util.HashMap;
import java.util.Objects;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.oceanbase.odc.common.json.JsonUtils;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/10
 */
@Converter
public class JsonObjectConverter implements AttributeConverter<Object, String> {
    @Override
    public String convertToDatabaseColumn(Object attribute) {
        return Objects.isNull(attribute) ? "{}" : JsonUtils.toJson(attribute);
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        return Objects.isNull(dbData) ? new HashMap<>() : JsonUtils.fromJson(dbData, Object.class);
    }
}
