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

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(exclude = {"ordinalPosition"})
public class DBTableAbstractPartitionDefinition {

    private String name;
    @NotNull
    private DBTablePartitionType type;

    /**
     * Max values for LESS THAN condition <br>
     * - 对于 HASH/RANGE 分区，maxValue 是 INT 值； <br>
     * - 对于 COLUMNS 分区，maxValue 可能是其它类型
     */
    private List<String> maxValues;
    private List<List<String>> valuesList;
    private String comment;
    @Positive
    private Long maxRows;
    @Positive
    private Long minRows;
    private Integer ordinalPosition;
    /**
     * below MySQL special
     */
    private String dataDirectory;
    private String indexDirectory;

    public void fillValues(String description) {
        if (StringUtils.isNotBlank(description)) {
            if (type == DBTablePartitionType.LIST_COLUMNS || type == DBTablePartitionType.LIST) {
                setValuesList(DBSchemaAccessorUtil.parseListRangePartitionDescription(description));
            } else if (type == DBTablePartitionType.RANGE || type == DBTablePartitionType.HASH) {
                // 对于 RANGE 和 HASH 分区，表达式只允许一列 或者 一个表达式，所以不用做拆分
                setMaxValues(Arrays.asList(description));
            } else {
                // 对于 RANGE_COLUMNS 和 KEY 分区，表达式允许多列，且不允许表达式，所以用逗号来分割
                setMaxValues(Arrays.asList(StringUtils.split(description, ",")));
            }
        }
    }

}

