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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang.Validate;
import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.validate.ValidatorBuilder;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.config.OrganizationConfigEntity;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.OrganizationConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrganizationConfigUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Convert method from object to string
     *
     * @param object object
     * @return string value
     * @exception UnexpectedException exception will be thrown when convert failed
     */
    private static String writeValueAsString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Fail to convert object to string", e);
        }
    }

    /**
     * Convert method from string to object
     *
     * @param jsonValue string value
     * @param clazz class object
     * @return object value
     * @throws UnexpectedException exception will be thrown when convert failed
     */
    private static <T> T readValue(String jsonValue, Class<T> clazz) {
        try {
            return mapper.readValue(jsonValue, clazz);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Fail to convert string to object", e);
        }
    }

    public static OrganizationConfig convertToConfig(List<Configuration> configurations) {
        Validate.notNull(configurations, "configurations cannot be null for OrganizationConfigUtil#convertToConfig");
        Map<String, String> convertMap =
                configurations.stream().collect(Collectors.toMap(configuration -> {
                    String[] keys = configuration.getKey().split("\\.");
                    return keys[keys.length - 1];
                }, configuration -> configuration.getValue()));
        return readValue(writeValueAsString(convertMap), OrganizationConfig.class);
    }

    public static void validateOrganizationConfig(List<Configuration> configurations) {
        Validate.notNull(configurations, "configurations cannot be null");
        Map<String, String> convertMap =
                configurations.stream().collect(Collectors.toMap(configuration -> {
                    String[] keys = configuration.getKey().split("\\.");
                    return keys[keys.length - 1];
                }, configuration -> configuration.getValue()));
        try {
            OrganizationConfig organizationConfig = readValue(writeValueAsString(convertMap), OrganizationConfig.class);
            Set<ConstraintViolation<OrganizationConfig>> result =
                    ValidatorBuilder.buildFastFailValidator().validate(organizationConfig);
            if (result.size() != 0) {
                throw new ConstraintViolationException(result);
            }
        } catch (Exception ex) {
            throw new BadArgumentException("configuration is invalid", ex);
        }
    }


    public static <T> Configuration convertDO2DTO(T entity) {
        Configuration configuration = new Configuration();
        BeanUtils.copyProperties(entity, configuration);
        return configuration;
    }

    public static <T> List<Configuration> convertDO2DTO(List<T> entities) {
        List<Configuration> configurations = new ArrayList<>();
        for (T entity : entities) {
            configurations.add(convertDO2DTO(entity));
        }
        return configurations;
    }

    public static List<Configuration> mergeConfigurations(List<Configuration> defaultConfigurations,
            List<Configuration> updateConfigurations) {
        List<Configuration> mergedConfigurations = new ArrayList<>();
        for (Configuration defaultConfig : defaultConfigurations) {
            Configuration mergedConfig = new Configuration();
            mergedConfig.setKey(defaultConfig.getKey());
            for (Configuration updateConfig : updateConfigurations) {
                if (Objects.nonNull(updateConfig.getKey())
                        && Objects.equals(updateConfig.getKey(), defaultConfig.getKey())) {
                    if (Objects.nonNull(updateConfig.getValue())) {
                        mergedConfig.setValue(updateConfig.getValue());
                    }
                }
            }
            if (Objects.isNull(mergedConfig.getValue()) && Objects.nonNull(defaultConfig.getValue())) {
                mergedConfig.setValue(defaultConfig.getValue());
            }
            mergedConfigurations.add(mergedConfig);
        }
        return mergedConfigurations;
    }

    public static OrganizationConfigEntity convertDTO2DO(Configuration config) {
        OrganizationConfigEntity entity = new OrganizationConfigEntity();
        BeanUtils.copyProperties(config, entity);
        return entity;
    }
}
