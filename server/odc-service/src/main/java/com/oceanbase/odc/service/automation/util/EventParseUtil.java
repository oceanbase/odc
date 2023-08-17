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
package com.oceanbase.odc.service.automation.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.jayway.jsonpath.JsonPath;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;

public class EventParseUtil {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    public static Object parseObject(Object source, String expression) {
        if (source instanceof Map || source instanceof List || source.getClass().isArray()) {
            return parseMap(source, expression);
        }
        return source;
    }

    public static Object parseMap(Object source, String expression) {
        JsonPath path = JsonPath.compile(expression);
        return path.read(JsonUtils.toJson(source));
    }

    /**
     * This method can only parse operation of {@code String}、{@code Array}、{@code List}. Like:
     * ["基础技术部", "contains", "技术部"] will be parsed into "'基础技术部'.contains('技术部')"
     *
     * @return result of {@param operation}
     */
    public static boolean validate(Object root, String operation, Object value) {
        if (root.getClass().isArray()) {
            String json = JsonUtils.toJson(root);
            root = JsonUtils.fromJsonList(json, String.class);
        }
        String assertStr = "'" + root.toString() + "'." + operation + "('" + value.toString() + "')";
        Object result = PARSER.parseExpression(assertStr).getValue();
        if (Objects.isNull(result)) {
            throw new UnexpectedException(String.format("Validate condition failed, assertStr=%s", assertStr));
        }
        return (boolean) result;
    }

    /**
     * Try not to use this method unless necessary
     */
    public static Map<String, Object> castToMap(Object any) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();
        Class<?> aClass = any.getClass();
        Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            map.put(field.getName(), field.get(any));
            field.setAccessible(false);
        }
        return map;
    }

}
