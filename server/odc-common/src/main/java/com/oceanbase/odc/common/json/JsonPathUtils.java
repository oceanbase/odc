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
package com.oceanbase.odc.common.json;

import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class JsonPathUtils {
    private static final JacksonJsonProvider JSON_PROVIDER = new JacksonJsonProvider();
    private static final ObjectMapper OBJECT_MAPPER = JacksonFactory.unsafeJsonMapper();
    private static final JacksonMappingProvider MAPPING_PROVIDER = new JacksonMappingProvider(OBJECT_MAPPER);
    private static final Configuration DEFAULT_CONFIG =
            Configuration.builder().jsonProvider(JSON_PROVIDER).mappingProvider(MAPPING_PROVIDER).build();

    public static Object read(String json, String path) {
        return JsonPath.using(DEFAULT_CONFIG).parse(json).read(path);
    }

    public static <T> T read(String json, String path, Class<T> classType) {
        return JsonPath.using(DEFAULT_CONFIG).parse(json).read(path, classType);
    }

    public static <T> T read(String json, String path, TypeRef<T> typeRef) {
        return JsonPath.using(DEFAULT_CONFIG).parse(json).read(path, typeRef);
    }

    public static <T> List<T> readList(String json, String path, Class<T> classType) {
        return JsonPath.using(DEFAULT_CONFIG).parse(json).read(path, new TypeRef<List<T>>() {
            @Override
            public Type getType() {
                return OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, classType);
            }
        });
    }
}
