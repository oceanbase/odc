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
package com.oceanbase.odc.core.datamasking.algorithm;

import java.io.Serializable;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2022/8/27
 */

@Data
public class Segment implements Serializable {
    /**
     * 此段是否需要替换
     */
    private Boolean mask;

    /**
     * 此段划分策略的类型，枚举值：DIGIT, DIGIT_PERCENTAGE, LEFT_OVER, DELIMITER
     */
    private SegmentType type;

    /**
     * 此段的替换字符集，当 mask = true 替换字符集的指定才有意义
     */
    private String replacedCharacters;

    /**
     * 按照 分隔符 划分脱敏内容段
     */
    private String delimiter;
    /**
     * 按照 指定位数的百分比 划分脱敏内容段
     */
    private Integer digitPercentage;
    /**
     * 按照 指定位数 划分脱敏内容段
     */
    private Integer digitNumber;
    /**
     * segment对应的内容串
     */
    private String content;
}
