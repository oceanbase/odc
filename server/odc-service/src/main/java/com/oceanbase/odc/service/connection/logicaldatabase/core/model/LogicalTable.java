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
package com.oceanbase.odc.service.connection.logicaldatabase.core.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/3/22 11:36
 * @Description: []
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogicalTable {
    private String name;

    private String fullNameExpression;

    private String databaseNamePattern;

    private String tableNamePattern;

    private List<DataNode> actualDataNodes;

    public Map<ConnectionConfig, List<DataNode>> groupByDataSource() {
        if (CollectionUtils.isEmpty(actualDataNodes)) {
            return Collections.emptyMap();
        }
        return actualDataNodes.stream().collect(Collectors.groupingBy(DataNode::getDataSourceConfig));
    }

    /**
     * Generate table name from pattern. Possible results: test_[#] --> test, test_[#]_t --> test_t,
     * test_[#]_[#] --> test, [#]_test --> test, test_[#]_[#]_t -> test_t
     */
    public String getName() {
        if (StringUtils.isEmpty(tableNamePattern)) {
            return StringUtils.EMPTY;
        }
        // handle potential sequences of [#] and surrounding underscores
        String name = tableNamePattern.replaceAll("([_]?\\[#\\]_?)+", "_");
        // remove redundant consecutive underscores (preserve one if necessary)
        name = name.replaceAll("_{2,}", "_");
        // remove any extra underscores that may be at the beginning or end of the string
        name = name.replaceAll("^_|_$", "");
        return name;
    }
}
