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

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.oceanbase.odc.common.json.JacksonFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/2/23 下午2:35
 * @Description: []
 */
@Slf4j
public class YamlUtils {
    private static final ObjectMapper OBJECT_MAPPER = JacksonFactory.yamlMapper();

    public static <T> T fromYaml(String srcPath, TypeReference<T> valueTypeRef) {
        URL url = ResourceUtils.class.getClassLoader().getResource(srcPath);
        if (url == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(url, valueTypeRef);
        } catch (IOException ex) {
            log.warn("failed to read yaml file, reason={}", ex.getMessage());
            return null;
        }
    }

    public static <T> List<T> fromYamlList(String srcPath, Class classType) {
        URL url = ResourceUtils.class.getClassLoader().getResource(srcPath);
        if (url == null) {
            return null;
        }
        try {
            CollectionType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, classType);
            return OBJECT_MAPPER.readValue(url, javaType);
        } catch (IOException ex) {
            log.warn("failed to read yaml file, reason={}", ex.getMessage());
            return null;
        }
    }

    public static <T> T fromYaml(URL url, Class<T> classType) {
        if (url == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(url, classType);
        } catch (IOException ex) {
            log.warn("failed to read yaml file, reason={}", ex.getMessage());
            return null;
        }
    }

    public static <T> T from(@NonNull String yamlStr, TypeReference<T> typeRef) {
        return from(yamlStr, typeRef, (prefix, name) -> true);
    }

    public static <T> T from(@NonNull String yamlStr, Class<T> clazz) {
        return from(yamlStr, clazz, (prefix, name) -> true);
    }

    public static <T> T from(@NonNull String yamlStr, Class<T> clazz, MapperUtils.PathMatcher judger) {
        return from(yamlStr, new TypeReference<T>() {
            @Override
            public Type getType() {
                return clazz;
            }
        }, judger);
    }

    public static <T> T from(@NonNull String yamlStr, TypeReference<T> typeRef, MapperUtils.PathMatcher judger) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            HashMap<String, Object> yaml = mapper.readValue(yamlStr, new TypeReference<HashMap<String, Object>>() {});
            return MapperUtils.get(yaml, typeRef, judger);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse from the yaml string", e);
            throw new RuntimeException(e);
        }
    }

}
