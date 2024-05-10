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

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(
        exclude = {"name", "warning", "owner", "schemaName", "tableName", "createTime", "updateTime",
                "ordinalPosition", "ddl", "visible"})
public class DBTableIndex implements DBObject, DBObjectWarningDescriptor {
    /**
     * 所属 schemaName
     */
    private String schemaName;

    /**
     * 所属 tableName
     */
    private String tableName;

    private String name;
    private DBIndexType type;
    private DBIndexAlgorithm algorithm;
    private String comment;
    /**
     * 是否全局索引，不设置时默认为 local
     */
    private Boolean global;
    /**
     * 是否唯一索引
     */
    private Boolean unique;
    /**
     * 是否主键
     */
    private Boolean primary;
    /**
     * 是否可见，不可见索引不会被优化器用于匹配执行计划，只有当 HINT 显式指定时才会命中
     */
    private Boolean visible;

    private List<String> columnNames;
    private String additionalInfo;
    private String compressInfo;

    private Boolean computeStatistics;

    private Integer ordinalPosition;

    private boolean nonUnique;
    private Long cardinality;
    /**
     * 是否可用，索引创建成功后是可用状态，否则不可用
     */
    private Boolean available;

    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp updateTime;
    private String owner;

    /**
     * MySQL special. How the column is sorted in the index. This can have values A (ascending), D
     * (descending), or NULL (not sorted).
     */
    private String collation;

    /**
     * MySQL special, can be used only with FULLTEXT indexes
     */
    private String parserName;

    private Long keyBlockSize;

    private String warning;
    /**
     * Oracle special
     */
    private String ddl;

    private List<DBColumnGroupElement> columnGroups;

    @Override
    public String name() {
        return name;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.INDEX;
    }
}

