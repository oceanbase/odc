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
package com.oceanbase.odc.service.config.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.config.UserConfigDO;
import com.oceanbase.odc.service.config.model.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Util of config object
 *
 * @author yh263208
 * @date 2021-05-28 22:24
 * @since ODC_release_2.4.2
 */
@Slf4j
public class ConfigObjectUtil {
    /**
     * Operator interface
     *
     * @author yh263208
     * @date 2021-05-29 01:52
     * @since ODC-release_2.4.2
     */
    interface ReturnValueOperator<T> {
        /**
         * Return method, used to return a value by config key and value
         *
         * @param configKey config key
         * @param configValue config value
         * @param description description of config
         */
        T doReturn(String configKey, String configValue, String description);
    }

    /**
     * Custom Function impl
     *
     * @author yh263208
     * @date 2021-05-29 01:53
     * @since ODC_release_2.4.2
     */
    static class CustomFunction<T> implements Function<Entry<String, Object>, T> {
        /**
         * Return Operator
         */
        private final ReturnValueOperator<T> operator;
        /**
         * config class
         */
        private final Class configClass;

        public CustomFunction(Class configClass, ReturnValueOperator<T> operator) {
            this.operator = operator;
            this.configClass = configClass;
        }

        @Override
        public T apply(Entry<String, Object> stringObjectEntry) {
            try {
                Validate.notNull(stringObjectEntry.getValue(),
                        "ConfigObject's value can not be null for convert method");
                String fieldName = stringObjectEntry.getKey();
                Field field = configClass.getDeclaredField(fieldName);
                String namespacePrefix = "";
                String description = "";
                ConfigMetaInfo configMetaInfo = field.getDeclaredAnnotation(ConfigMetaInfo.class);
                if (configMetaInfo != null) {
                    String prefix = configMetaInfo.prefix();
                    namespacePrefix = StringUtils.isBlank(prefix) ? "" : prefix + ".";
                    description = configMetaInfo.description();
                }
                String configKey = namespacePrefix + stringObjectEntry.getKey();
                String configValue;
                Class clazz = stringObjectEntry.getValue().getClass();
                if (clazz.getClassLoader() == null || clazz.isPrimitive() || clazz.isEnum()) {
                    if (clazz.isArray()) {
                        configValue = mapper.writeValueAsString(stringObjectEntry.getValue());
                    } else {
                        configValue = stringObjectEntry.getValue().toString();
                    }
                } else {
                    configValue = mapper.writeValueAsString(stringObjectEntry.getValue());
                }
                return this.operator.doReturn(configKey, configValue, description);
            } catch (Throwable e) {
                log.error("Fail to map entry for convert method, entryKey={}, entryValue={}",
                        stringObjectEntry.getKey(),
                        stringObjectEntry.getValue());
            }
            return null;
        }
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Convert method from object to string
     *
     * @param object object
     * @return string value
     * @exception UnexpectedException exception will be thrown when convert failed
     */
    private static String writeValueAsString(Object object) throws UnexpectedException {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Fail to convert object to string", e);
        }
    }

    /**
     * Converto method from string to object
     *
     * @param jsonValue string value
     * @param clazz class object
     * @return object value
     * @throws UnexpectedException exception will be thrown when convert failed
     */
    private static <T> T readValue(String jsonValue, Class<T> clazz) throws UnexpectedException {
        try {
            return mapper.readValue(jsonValue, clazz);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Fail to convert string to object", e);
        }
    }

    /**
     * Convert method, from config object to Configuration
     *
     * @param configObject config object
     * @return list of config dto
     */
    public static <T> List<Configuration> convertToDTO(T configObject) throws UnexpectedException {
        Validate.notNull(configObject, "ConfigObject can not be null for ConfigObjectUtil#convertToDTO");
        HashMap<String, Object> convertMap = readValue(writeValueAsString(configObject), HashMap.class);
        return convertMap.entrySet().stream()
                .map(new CustomFunction<>(configObject.getClass(),
                        (configKey, configValue, description) -> new Configuration(configKey, configValue)))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Convert method, from config object to OdcConfigDO
     *
     * @param configObject config object
     * @return list of config dto
     */
    public static <T> List<UserConfigDO> convertToDO(T configObject) throws UnexpectedException {
        Validate.notNull(configObject, "ConfigObject can not be null for ConfigObjectUtil#convertToDTO");
        HashMap<String, Object> convertMap = readValue(writeValueAsString(configObject), HashMap.class);
        return convertMap.entrySet().stream()
                .map(new CustomFunction<>(configObject.getClass(),
                        (configKey, configValue, description) -> new UserConfigDO(configKey, configValue, description)))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Set value from DTO
     *
     * @param configurations config dto list
     * @param configObject config object
     * @return new config object
     */
    public static <T> T setConfigObjectFromDTO(List<Configuration> configurations, T configObject)
            throws UnexpectedException {
        Validate.notNull(configurations, "ConfigDTO list can not be null for ConfigObjectUtil#setConfigObjectFromDTO");
        Validate.notNull(configObject, "Config object can not be null for ConfigObjectUtil#setConfigObjectFromDTO");
        List<Configuration> originDTOList = convertToDTO(configObject);
        Map<String, String> convertMap = originDTOList.stream().collect(Collectors.toMap(configDTO -> {
            String[] keys = configDTO.getKey().split("\\.");
            return keys[keys.length - 1];
        }, configDTO -> {
            for (Configuration item : configurations) {
                if (item != null && configDTO.getKey().equals(item.getKey())) {
                    if (item.getValue() != null) {
                        return item.getValue();
                    }
                }
            }
            return configDTO.getValue();
        }));
        return (T) readValue(writeValueAsString(convertMap), configObject.getClass());
    }

    /**
     * Set value from DO
     *
     * @param configDOList config do list
     * @param configObject config object
     * @return new config object
     */
    public static <T> T setConfigObjectFromDO(List<UserConfigDO> configDOList, T configObject)
            throws UnexpectedException {
        Validate.notNull(configDOList, "ConfigDO list can not be null for ConfigObjectUtil#setConfigObjectFromDO");
        Validate.notNull(configObject, "Config object can not be null for ConfigObjectUtil#setConfigObjectFromDO");
        List<UserConfigDO> originDOList = convertToDO(configObject);
        Map<String, String> convertMap = originDOList.stream().collect(Collectors.toMap(odcConfigDO -> {
            String[] keys = odcConfigDO.getKey().split("\\.");
            return keys[keys.length - 1];
        }, configDO -> {
            for (UserConfigDO item : configDOList) {
                if (item != null && configDO.getKey().equals(item.getKey())) {
                    if (item.getValue() != null) {
                        return item.getValue();
                    }
                }
            }
            return configDO.getValue();
        }));
        return (T) readValue(writeValueAsString(convertMap), configObject.getClass());
    }

}
