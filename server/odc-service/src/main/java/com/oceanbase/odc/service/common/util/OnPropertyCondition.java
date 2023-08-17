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
package com.oceanbase.odc.service.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * {@link OnPropertyCondition}
 *
 * @author yh263208
 * @date 2022-05-19 20:29
 * @since ODC_release_3.3.1
 * @see Condition
 */
public class OnPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MultiValueMap<String, Object> multiValueMap =
                metadata.getAllAnnotationAttributes(ConditionalOnProperty.class.getName());
        if (multiValueMap == null) {
            throw new IllegalStateException("Can't find any attributes for @ConditionalOnProperty");
        }
        return annotationMultiValueMap(multiValueMap).stream()
                .allMatch(attr -> isMatch(attr, context.getEnvironment()));
    }

    private List<AnnotationAttributes> annotationMultiValueMap(MultiValueMap<String, Object> multiValueMap) {
        List<Map<String, Object>> maps = new ArrayList<>();
        multiValueMap.forEach((key, value) -> {
            for (int i = 0; i < value.size(); i++) {
                Map<String, Object> map;
                if (i < maps.size()) {
                    map = maps.get(i);
                } else {
                    map = new HashMap<>();
                    maps.add(map);
                }
                map.put(key, value.get(i));
            }
        });
        List<AnnotationAttributes> annotationAttributes = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            annotationAttributes.add(AnnotationAttributes.fromMap(map));
        }
        return annotationAttributes;
    }

    private boolean isMatch(AnnotationAttributes annotationAttributes, PropertyResolver resolver) {
        AnnotationSpec spec = new AnnotationSpec(annotationAttributes);
        return Arrays.stream(spec.getNames()).map(name -> spec.getPrefix() + name).anyMatch(key -> {
            if (resolver.containsProperty(key)) {
                return isMatch(resolver.getProperty(key), spec.getHavingValues(), spec.isCollectionProperty());
            }
            return spec.isMatchIfMissing();
        });
    }

    private boolean isMatch(String value, String[] requiredValues, boolean collectionProperty) {
        if (requiredValues != null && requiredValues.length != 0) {
            if (collectionProperty) {
                Set<LowerCasePropertyValue> valueSet =
                        Arrays.stream(value.split(",")).map(LowerCasePropertyValue::new).collect(Collectors.toSet());
                return Arrays.stream(requiredValues).map(LowerCasePropertyValue::new).anyMatch(valueSet::contains);
            }
            return Arrays.stream(requiredValues).anyMatch(required -> required.equalsIgnoreCase(value));
        }
        return !"false".equalsIgnoreCase(value);
    }

    /**
     * equalsIgnoreCase
     */
    @EqualsAndHashCode
    static class LowerCasePropertyValue {

        @Getter
        private String value;

        private LowerCasePropertyValue() {};

        public LowerCasePropertyValue(String value) {
            this.value = value.toLowerCase();
        }
    }

    @Getter
    private static class AnnotationSpec {

        private final String prefix;
        private final String[] havingValues;
        private final String[] names;
        private final boolean matchIfMissing;
        private final boolean collectionProperty;

        AnnotationSpec(AnnotationAttributes annotationAttributes) {
            String prefix = annotationAttributes.getString("prefix").trim();
            if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
                prefix = prefix + ".";
            }
            this.prefix = prefix;
            this.havingValues = annotationAttributes.getStringArray("havingValues");
            this.names = getNames(annotationAttributes);
            this.matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");
            this.collectionProperty = annotationAttributes.getBoolean("collectionProperty");
        }

        private String[] getNames(AnnotationAttributes annotationAttributes) {
            String[] value = annotationAttributes.getStringArray("value");
            String[] name = annotationAttributes.getStringArray("name");
            if (value.length == 0 && name.length == 0) {
                throw new IllegalArgumentException("The name or value attribute must be specified");
            }
            if (value.length != 0 && name.length != 0) {
                throw new IllegalArgumentException("The name and value attributes are exclusive");
            }
            return (value.length > 0) ? value : name;
        }
    }

}
