/*
 * Copyright (c) 2025 OceanBase.
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
package com.oceanbase.odc.service.datasecurity.model;

import java.util.Optional;

/**
 * 13 Default Sensitivity Types Identified by AI
 * 
 * @author fenyf
 * @date 2025/8/1
 */
public enum DefaultSensitiveType {

    PERSONAL_NAME_CHINESE("${com.oceanbase.odc.builtin-resource.masking-algorithm.personal-name-chinese.name}"),

    PERSONAL_NAME_ALPHABET("${com.oceanbase.odc.builtin-resource.masking-algorithm.personal-name-alphabet.name}"),

    NICKNAME("${com.oceanbase.odc.builtin-resource.masking-algorithm.nickname.name}"),

    EMAIL("${com.oceanbase.odc.builtin-resource.masking-algorithm.email.name}"),

    ADDRESS("${com.oceanbase.odc.builtin-resource.masking-algorithm.address.name}"),

    PHONE_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.phone-number.name}"),

    FIXED_LINE_PHONE_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.fixed-line-phone-number.name}"),

    CERTIFICATE_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.certificate-number.name}"),

    BANK_CARD_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.bank-card-number.name}"),

    LICENSE_PLATE_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.license-plate-number.name}"),

    DEVICE_ID("${com.oceanbase.odc.builtin-resource.masking-algorithm.device-id.name}"),

    IP("${com.oceanbase.odc.builtin-resource.masking-algorithm.ip.name}"),

    MAC("${com.oceanbase.odc.builtin-resource.masking-algorithm.mac.name}");

    private final String algorithmName;

    DefaultSensitiveType(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public static boolean isDefaultType(String sensitiveType) {
        return findBestMatch(sensitiveType).isPresent();
    }

    /**
     * Retrieve the corresponding algorithm name based on the name of the sensitive type.
     */
    public static Optional<String> getAlgorithmNameBySensitiveType(String sensitiveType) {
        return findBestMatch(sensitiveType).map(DefaultSensitiveType::getAlgorithmName);
    }

    public static Optional<DefaultSensitiveType> getByDisplayName(String sensitiveType) {
        return findBestMatch(sensitiveType);
    }

    /**
     * Match sensitive type
     */
    public static Optional<DefaultSensitiveType> findBestMatch(String sensitiveType) {
        if (sensitiveType == null || sensitiveType.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = sensitiveType.toLowerCase().trim();

        for (DefaultSensitiveType type : values()) {
            if (type.name().toLowerCase().equals(normalized)) {
                return Optional.of(type);
            }
        }

        for (DefaultSensitiveType type : values()) {
            String hyphenFormat = type.name().toLowerCase().replace("_", "-");
            if (hyphenFormat.equals(normalized)) {
                return Optional.of(type);
            }
        }

        for (DefaultSensitiveType type : values()) {
            String enumName = type.name().toLowerCase();
            String hyphenFormat = enumName.replace("_", "-");
            if (enumName.contains(normalized) || normalized.contains(enumName.replace("_", "")) ||
                    hyphenFormat.contains(normalized) || normalized.contains(hyphenFormat.replace("-", ""))) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

}
