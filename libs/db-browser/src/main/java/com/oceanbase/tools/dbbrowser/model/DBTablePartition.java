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
import lombok.EqualsAndHashCode;

/**
 * @Author: Lebie
 * @Date: 2022/8/17 下午12:02
 * @Description: []
 */
@Data
@EqualsAndHashCode(exclude = {"name", "warning", "subpartition"})
public class DBTablePartition implements DBObject, DBObjectWarningDescriptor {
    /**
     * 所属 schemaName
     */
    private String schemaName;

    /**
     * 所属 tableName
     */
    private String tableName;
    private DBTablePartitionOption partitionOption;
    private List<DBTablePartitionDefinition> partitionDefinitions;
    private String warning;

    private Boolean subpartitionTemplated;

    /**
     * 二级分区
     */
    private DBTablePartition subpartition;

    @Override
    public String name() {
        return null;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.PARTITION;
    }
}
