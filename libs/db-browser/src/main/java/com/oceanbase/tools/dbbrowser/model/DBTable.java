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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString.Exclude;

@Data
@EqualsAndHashCode(of = {"tableOptions"})
public class DBTable implements DBObject, DBObjectWarningDescriptor {
    /**
     * 所属 schema，API 层面不可见
     */
    @JsonIgnore
    @Exclude
    private DBSchema schema;

    private String name;

    private DBTableOptions tableOptions;
    private DBTableStats stats;

    private List<DBTableColumn> columns;
    private List<DBTableIndex> indexes;
    /**
     * 约束、包含 主键约束、check 约束、外键约束
     */
    private List<DBTableConstraint> constraints;
    private DBTablePartition partition;

    private List<DBColumnGroupElement> columnGroups;

    @JsonProperty(access = Access.READ_ONLY)
    private String DDL;
    private String owner;
    private String schemaName;

    private String warning;

    @Data
    public static class DBTableOptions {
        private String charsetName;
        private String collationName;
        private String comment;
        private Boolean encryption;
        @JsonProperty(access = Access.READ_ONLY)
        private Timestamp createTime;
        @JsonProperty(access = Access.READ_ONLY)
        private Timestamp updateTime;
        /**
         * initial value for auto increment column
         */
        private Long autoIncrementInitialValue;

        /**
         * below OceanBase special, may fetch from `show table status like 'table_name', the
         * `Create_Options` field;
         */
        private String primaryZone;
        private String locality;
        private Integer replicaNum;
        private String tablegroupName;
        private String rowFormat;
        private String compressionOption;
        private Integer blockSize;
        private Long tabletSize;
        private Boolean useBloomFilter;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.TABLE;
    }
}

