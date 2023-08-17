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
package com.oceanbase.odc.core.datamasking.config;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2022/8/3
 */

@Data
@Builder
public class FieldConfig {
    public static final int DEFAULT_MAX_SINGLE_VALUE_LENGTH = 1024;
    public static final int DEFAULT_SINGLE_VALUE_TIMEOUT_SECONDS = 5;
    public static final String DEFAULT_VALUE_AFTER_FAILURE = "****";
    /**
     * 指定脱敏的字段名称
     */
    private String fieldName;
    /**
     * 指定使用的脱敏算法
     */
    private String algorithmType;

    /**
     * 指定脱敏算法相关参数
     */
    private Map<String, Object> algorithmParams;

    /**
     * 脱敏处理单条内容的最大字符长度
     */
    private Integer maxLengthForSingleValue = DEFAULT_MAX_SINGLE_VALUE_LENGTH;

    /**
     * 脱敏处理单条内容的最长时间
     */
    private Integer singleMaskingTimeoutSeconds = DEFAULT_SINGLE_VALUE_TIMEOUT_SECONDS;

    /**
     * 脱敏处理单条内容失败后的默认替换值
     */
    private String defaultValueAfterFailure = DEFAULT_VALUE_AFTER_FAILURE;
}
