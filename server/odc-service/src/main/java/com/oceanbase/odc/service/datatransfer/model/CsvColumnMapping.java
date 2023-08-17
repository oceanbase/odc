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
 * csv file mapping
 *
 * @author yh263208
 * @date 2021-03-22 17:59
 * @since ODC_release_2.4.1
 */
@Getter
@Setter
@ToString
public class CsvColumnMapping {
    private Integer srcColumnPosition;
    private String srcColumnName;
    private String firstLineValue;
    private String destColumnName;
    private String destColumnType;
    private Integer destColumnPosition;
}
