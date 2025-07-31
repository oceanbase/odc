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

import java.util.Arrays;
import java.util.Optional;

/**
 * 默认敏感类型枚举
 * 定义AI识别的13种默认敏感类型，每种类型对应一个同名的脱敏算法
 * 支持多语言匹配和模糊匹配，提高AI识别结果的容错率
 *
 * @author AI Assistant
 * @date 2024/01/01
 */
public enum DefaultSensitiveType {

    /**
     * 个人姓名（汉字类型）
     */
    PERSONAL_NAME_CHINESE("${com.oceanbase.odc.builtin-resource.masking-algorithm.personal-name-chinese.name}"),

    /**
     * 个人姓名（字母类型）
     */
    PERSONAL_NAME_ALPHABET("${com.oceanbase.odc.builtin-resource.masking-algorithm.personal-name-alphabet.name}"),

    /**
     * 昵称
     */
    NICKNAME("${com.oceanbase.odc.builtin-resource.masking-algorithm.nickname.name}"),

    /**
     * 邮箱
     */
    EMAIL("${com.oceanbase.odc.builtin-resource.masking-algorithm.email.name}"),

    /**
     * 地址
     */
    ADDRESS("${com.oceanbase.odc.builtin-resource.masking-algorithm.address.name}"),

    /**
     * 手机号码
     */
    PHONE_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.phone-number.name}"),

    /**
     * 固定电话
     */
    FIXED_LINE_PHONE_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.fixed-line-phone-number.name}"),

    /**
     * 证件号码
     */
    CERTIFICATE_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.certificate-number.name}"),

    /**
     * 银行卡号
     */
    BANK_CARD_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.bank-card-number.name}"),

    /**
     * 车牌号
     */
    LICENSE_PLATE_NUMBER("${com.oceanbase.odc.builtin-resource.masking-algorithm.license-plate-number.name}"),

    /**
     * 设备唯一识别号
     */
    DEVICE_ID("${com.oceanbase.odc.builtin-resource.masking-algorithm.device-id.name}"),

    /**
     * IP地址
     */
    IP("${com.oceanbase.odc.builtin-resource.masking-algorithm.ip.name}"),

    /**
     * MAC地址
     */
    MAC("${com.oceanbase.odc.builtin-resource.masking-algorithm.mac.name}");

    private final String algorithmName;

    DefaultSensitiveType(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * 判断给定的敏感类型是否为默认类型（智能匹配）
     *
     * @param sensitiveType 敏感类型名称
     * @return 如果是默认类型返回true，否则返回false
     */
    public static boolean isDefaultType(String sensitiveType) {
        return findBestMatch(sensitiveType).isPresent();
    }

    /**
     * 根据敏感类型名称获取对应的脱敏算法名称（智能匹配）
     *
     * @param sensitiveType 敏感类型名称
     * @return 对应的脱敏算法名称，如果不是默认类型则返回空
     */
    public static Optional<String> getAlgorithmNameBySensitiveType(String sensitiveType) {
        return findBestMatch(sensitiveType).map(DefaultSensitiveType::getAlgorithmName);
    }

    /**
     * 根据敏感类型名称获取对应的枚举值（智能匹配）
     *
     * @param sensitiveType 敏感类型名称
     * @return 对应的枚举值，如果不是默认类型则返回空
     */
    public static Optional<DefaultSensitiveType> getByDisplayName(String sensitiveType) {
        return findBestMatch(sensitiveType);
    }

    /**
     * 智能匹配敏感类型（支持多种匹配策略）
     *
     * @param sensitiveType 敏感类型名称
     * @return 匹配到的枚举值
     */
    public static Optional<DefaultSensitiveType> findBestMatch(String sensitiveType) {
        if (sensitiveType == null || sensitiveType.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = sensitiveType.toLowerCase().trim();

        // 精确匹配枚举名称（下划线格式）
        for (DefaultSensitiveType type : values()) {
            if (type.name().toLowerCase().equals(normalized)) {
                return Optional.of(type);
            }
        }

        // 精确匹配连字符格式（AI返回的格式）
        for (DefaultSensitiveType type : values()) {
            String hyphenFormat = type.name().toLowerCase().replace("_", "-");
            if (hyphenFormat.equals(normalized)) {
                return Optional.of(type);
            }
        }

        // 模糊匹配：检查是否包含关键词
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