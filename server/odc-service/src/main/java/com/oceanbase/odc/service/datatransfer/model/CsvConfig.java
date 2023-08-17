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
package com.oceanbase.odc.service.datatransfer.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * CSV file configuration
 *
 * @date 2021-03-23 11:00
 * @since ODC_release_2.4.1
 */
@Getter
@Setter
@ToString
public class CsvConfig {
    private EncodingType encoding = EncodingType.UTF_8;
    private String fileName;
    /**
     * flag to illustrate whether convert empty string to null
     */
    private boolean blankToNull;
    /**
     * flag to illustrate whether skip csv's header
     */
    private boolean skipHeader = true;
    private String columnSeparator = ",";
    private String lineSeparator = "\n";
    private String columnDelimiter = "'";
}
