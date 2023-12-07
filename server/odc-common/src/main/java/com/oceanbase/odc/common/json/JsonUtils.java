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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.json.XML;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.oceanbase.odc.common.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Json to String convert, same API as Gson, leverage Jackson, but catch exception, return null
 * instead
 *
 * @author yizhou.xw
 * @version : JsonUtils.java, v 0.1 2020-02-20 9:02
 */
@Slf4j
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = JacksonFactory.jsonMapper();
    private static final ObjectMapper OBJECT_MAPPER_PRETTY = JacksonFactory.jsonMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private static final ObjectMapper OBJECT_MAPPER_UPPER_CAMEL_CASE = JacksonFactory.jsonMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
            .setSerializationInclusion(Include.NON_NULL);

    private static final ObjectMapper OBJECT_MAPPER_IGNORE_MISSING_PROPERTY = JacksonFactory.jsonMapper()
            .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false);

    private static final ObjectMapper UNSAFE_OBJECT_MAPPER = JacksonFactory.unsafeJsonMapper();

    public static <T> T fromJson(String json, Class<T> classType) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, classType);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> valueTypeRef) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, valueTypeRef);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Ignore deserialize fail when json missing property annotation by @JsonTypeInfo
     * 
     * @param json json string
     * @param valueTypeRef represent the actual type of generic
     * @return object represented by valueTypeRef
     */
    public static <T> T fromJsonIgnoreMissingProperty(String json, TypeReference<T> valueTypeRef) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER_IGNORE_MISSING_PROPERTY.readValue(json, valueTypeRef);
        } catch (JsonProcessingException e) {
            return null;
        }
    }


    /**
     * from jsonString to a List object
     *
     * @param json jsonString which is a json array
     * @param classType classType of the List
     * @return List of classType, null if input json is null or invalid
     */
    public static <T> List<T> fromJsonList(String json, Class<T> classType) {
        if (json == null) {
            return null;
        }
        try {
            CollectionType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, classType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * from jsonString to a Map object
     */
    public static <K, V> Map<K, V> fromJsonMap(String json, Class<K> keyType, Class<V> valueType) {
        if (json == null) {
            return null;
        }
        try {
            MapType mapType = OBJECT_MAPPER.getTypeFactory().constructMapType(HashMap.class, keyType, valueType);
            return OBJECT_MAPPER.readValue(json, mapType);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static String toJson(Object obj) {
        return innerToJson(OBJECT_MAPPER, obj);
    }

    /**
     * 不会将 @MaskField 注解修饰的字段处理成指定模糊字段(******)
     *
     * @param obj 待序列化成Json的对象
     * @return 带敏感信息明文的Json串
     * @see MaskOutput
     */
    public static String unsafeToJson(Object obj) {
        return innerToJson(UNSAFE_OBJECT_MAPPER, obj);
    }

    public static String prettyToJson(Object obj) {
        return innerToJson(OBJECT_MAPPER_PRETTY, obj);
    }

    public static String toJsonUpperCamelCase(Object obj) {
        return innerToJson(OBJECT_MAPPER_UPPER_CAMEL_CASE, obj);
    }

    /**
     * 将 XML 转换成 JSON
     */
    public static String xmlToJson(String xml) {
        if (StringUtils.isBlank(xml)) {
            return xml;
        }
        try {
            return XML.toJSONObject(xml).toString();
        } catch (Exception e) {
            log.error("failed to convert json to xml string, reason:{}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 JSON 转换成 XML
     */
    public static String jsonToXml(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            return XML.toString(jsonObject);
        } catch (Exception e) {
            log.error("failed to convert json to xml string, reason:{}", e.getMessage());
            return null;
        }
    }

    private static String innerToJson(ObjectMapper objectMapper, Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("failed to convert to json string, reason:{}", e.getMessage());
            return null;
        }
    }
}
