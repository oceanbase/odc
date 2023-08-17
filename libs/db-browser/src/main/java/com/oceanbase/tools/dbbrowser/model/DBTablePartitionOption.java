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
package com.oceanbase.tools.dbbrowser.model;

import java.util.List;

import lombok.Data;

@Data
public class DBTablePartitionOption {
    private DBTablePartitionType type;
    /**
     * 表达式，注意只有 HASH/RANGE/LIST 分区类型支持表达式
     */
    private String expression;

    /**
     * 当 expression 为空时有效。 <br>
     * 对于 KEY 分区，尽管 expression 为空，但是 columnNames 可能同时为空，此时使用的是主键或者唯一性索引键。 <br>
     *
     */
    private List<String> columnNames;

    /**
     * 分区数，如果指定分区数 partitionsNum，那么 partition definition 不提供或者数量需要和 partitionsNum 一致。
     */
    private Integer partitionsNum;

    /**
     * below Oracle special
     */
    private Boolean automatic;
    private List<String> verticalColumnNames;

}
