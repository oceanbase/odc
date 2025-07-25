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
package com.oceanbase.odc.service.llm.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.oceanbase.odc.service.llm.provider.ModelCredential;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate;

public class YamlUtil {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static <T> T from(String yamlStr, Class<T> clazz) {
        try {
            return YAML_MAPPER.readValue(yamlStr, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse yaml string", e);
        }
    }

    public static List<ModelCredential> loadAllModelsFromResourceYaml(String resourcePath,
            Function<ModelTemplate, ModelCredential> converter) {
        List<ModelCredential> credentials = new ArrayList<>();
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(resourcePath);
            for (Resource resource : resources) {
                if (resource != null && !resource.getFilename().endsWith(".yaml")) {
                    continue;
                }
                String yamlContent = new String(resource.getInputStream().readAllBytes());
                ModelTemplate template = from(yamlContent, ModelTemplate.class);
                credentials.add(converter.apply(template));
            }
            return credentials;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load models", e);
        }
    }

    /**
     * 将Object转换为Float
     */
    public static Float getFloatValue(Object value) {
        if (value == null)
            return null;
        if (value instanceof Float)
            return (Float) value;
        if (value instanceof Double)
            return ((Double) value).floatValue();
        if (value instanceof Number)
            return ((Number) value).floatValue();
        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 将Object转换为Integer
     */
    public static Integer getIntegerValue(Object value) {
        if (value == null)
            return null;
        if (value instanceof Integer)
            return (Integer) value;
        if (value instanceof Number)
            return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 将Object转换为Boolean
     */
    public static Boolean getBooleanValue(Object value) {
        if (value == null)
            return null;
        if (value instanceof Boolean)
            return (Boolean) value;
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    /**
     * 将Object转换为String
     */
    public static String getStringValue(Object value) {
        if (value == null)
            return null;
        return value.toString();
    }

}
