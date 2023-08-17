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
package com.oceanbase.odc.service.config.model;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotBlank;

import com.oceanbase.odc.common.validate.ValidatorBuilder;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO for all configurations
 */
@Setter
@Getter
@ToString
@EqualsAndHashCode
public class Configuration {
    /**
     * Config key
     */
    @NotBlank(message = "Config key can not be null or blank")
    private String key;

    /**
     * Config value
     */
    private String value;

    /**
     * Default constructor for object
     *
     * @param configKey config key
     * @param configValue configValue
     */
    public Configuration(String configKey, String configValue) {
        this.key = configKey;
        this.value = configValue;
        Set<ConstraintViolation<Configuration>> result = ValidatorBuilder.buildFastFailValidator().validate(this);
        if (result.size() != 0) {
            throw new ConstraintViolationException(result);
        }
    }

    public Configuration() {}

}
