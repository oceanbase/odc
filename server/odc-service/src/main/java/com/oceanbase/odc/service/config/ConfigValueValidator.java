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
package com.oceanbase.odc.service.config;

import java.math.BigDecimal;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;
import com.oceanbase.odc.service.session.SessionProperties;

public class ConfigValueValidator {

    public static void validate(ConfigurationMeta meta, String value) {
        PreConditions.notNull(meta, "meta");
        checkValueNotNull(meta, value);
        checkValueAllowed(meta, value);
        checkValueMin(meta, value);
        checkValueMax(meta, value);
    }

    public static void validateOrganizationConfig(ConfigurationMeta meta, SessionProperties properties, String value) {
        PreConditions.notNull(meta, "meta");
        checkValueNotNull(meta, value);
        checkValueAllowed(meta, value);
        checkValueMin(meta, value);
        // check that the max query limit is less than the value of the metadata
        checkValueMaxBasedOnSystemConfig(properties, value);

    }

    private static void checkValueNotNull(ConfigurationMeta meta, String value) {
        if (!meta.isNullable() && Objects.isNull(value)) {
            throw new IllegalArgumentException(
                    String.format("Value cannot be null for key '%s'", meta.getKey()));
        }
    }

    private static void checkValueAllowed(ConfigurationMeta meta, String value) {
        if (CollectionUtils.isNotEmpty(meta.getAllowedValues())) {
            if (!meta.getAllowedValues().contains(value)) {
                throw new IllegalArgumentException(
                        String.format("Value is not allowed for key '%s', allowableValues are '%s'", meta.getKey(),
                                String.join(",", meta.getAllowedValues())));
            }
        }
    }

    private static void checkValueMax(ConfigurationMeta meta, String value) {
        if (Objects.nonNull(meta.getMaxValue())) {
            if (meta.getMaxValue().compareTo(new BigDecimal(value)) < 0) {
                throw new IllegalArgumentException(
                        String.format("Value is greater than max value for key '%s', maxValue is '%s'",
                                meta.getKey(), meta.getMaxValue()));
            }
        }
    }

    private static void checkValueMin(ConfigurationMeta meta, String value) {
        if (Objects.nonNull(meta.getMinValue())) {
            if (meta.getMinValue().compareTo(new BigDecimal(value)) > 0) {
                throw new IllegalArgumentException(
                        String.format("Value is less than min value for key '%s', minValue is '%s'",
                                meta.getKey(), meta.getMinValue()));
            }
        }
    }

    private static void checkValueMaxBasedOnSystemConfig(SessionProperties properties, String value) {
        String maxQueryLimit = String.valueOf(properties.getResultSetMaxRows());
        BigDecimal valueFromSystemConfig = new BigDecimal(maxQueryLimit);
        BigDecimal valueFromOrganization = new BigDecimal(value);
        if (valueFromSystemConfig.compareTo(valueFromOrganization) < 0) {
            throw new IllegalArgumentException(
                    String.format("Value is greater than max value for key, maxValue is '%s'",
                            properties.getResultSetMaxRows()));
        }
    }
}
