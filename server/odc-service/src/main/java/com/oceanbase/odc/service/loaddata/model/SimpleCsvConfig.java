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
package com.oceanbase.odc.service.loaddata.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单CSV配置类，用于CSV文件的解析和生成操作。 该类封装了多种设置，影响CSV数据的处理方式。
 * 
 * @author xien.sxe
 * @date 2024/3/4
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class SimpleCsvConfig implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -8266445970416731018L;

    /**
     * 字段分隔符，用于CSV记录中分隔字段，默认为逗号。
     */
    @NotNull
    private char fieldSeparator = ',';

    /**
     * 列分隔符，用于CSV记录中文本列的界定，默认为单引号。
     */
    @NotNull
    private char columnDelimiter = '\'';

    /**
     * 是否跳过CSV文件的表头行的标志，默认为 true。
     */
    @NotNull
    private boolean skipHeader = true;

    /**
     * 转义特殊字符的转义符，默认为反斜杠。
     */
    @NotNull
    private char escapeChar = '\\';

    /**
     * CSV文件中用于分隔行的字符串，默认为换行符。 根据操作系统的不同，该值可能会有所变化（例如，Windows：\r\n，Linux：\n）。
     */
    @NotNull
    private String lineSeparator = "\\n";
}

