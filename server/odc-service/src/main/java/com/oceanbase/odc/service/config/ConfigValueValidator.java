/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.config;

import java.math.BigDecimal;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;

public class ConfigValueValidator {

    public static void validate(ConfigurationMeta meta, String value) {
        PreConditions.notNull(meta, "meta");
        if (!meta.isNullable() && Objects.isNull(value)) {
            throw new IllegalArgumentException(
                    String.format("Value is null for key '%s', but the configuration is not nullable", meta.getKey()));
        }
        if (CollectionUtils.isNotEmpty(meta.getAllowedValues())) {
            if (!meta.getAllowedValues().contains(value)) {
                throw new IllegalArgumentException(
                        String.format("Value is not allowed for key '%s'", meta.getKey()));
            }
        }
        if (Objects.nonNull(meta.getMaxValue())) {
            if (meta.getMaxValue().compareTo(new BigDecimal(value)) < 0) {
                throw new IllegalArgumentException(
                        String.format("Value is greater than max value for key '%s'", meta.getKey()));
            }
        }
        if (Objects.nonNull(meta.getMinValue())) {
            if (meta.getMinValue().compareTo(new BigDecimal(value)) > 0) {
                throw new IllegalArgumentException(
                        String.format("Value is less than min value for key '%s'", meta.getKey()));
            }
        }
    }

}
