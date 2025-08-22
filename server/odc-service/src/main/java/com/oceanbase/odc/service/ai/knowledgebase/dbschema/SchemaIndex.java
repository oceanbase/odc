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
package com.oceanbase.odc.service.ai.knowledgebase.dbschema;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBView;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class SchemaIndex {
    /**
     * 名称
     */
    private String name;
    /**
     * 注释
     */
    private String comment;
    /**
     * 字段信息
     */
    private List<SchemaIndex> fields;
    /**
     * 索引定义，通常是数据库对象的 DDL
     */
    private String definition;

    public static SchemaIndex ofDBTableColumn(@NonNull DBTableColumn column) {
        SchemaIndex schemaIndex = new SchemaIndex();
        schemaIndex.setName(column.getName());
        schemaIndex.setComment(column.getComment());
        return schemaIndex;
    }

    public static SchemaIndex ofDBTable(@NonNull DBTable dbTable) {
        SchemaIndex schemaIndex = new SchemaIndex();
        schemaIndex.setName(dbTable.getName());
        if (dbTable.getTableOptions() != null) {
            schemaIndex.setComment(dbTable.getTableOptions().getComment());
        }
        if (dbTable.getColumns() != null) {
            schemaIndex.setFields(dbTable.getColumns().stream()
                    .map(SchemaIndex::ofDBTableColumn).collect(Collectors.toList()));
        }
        schemaIndex.setDefinition(dbTable.getDDL());
        return schemaIndex;
    }

    public static SchemaIndex ofDBView(@NonNull DBView dbView) {
        SchemaIndex schemaIndex = new SchemaIndex();
        schemaIndex.setName(dbView.getViewName());
        schemaIndex.setComment(dbView.getComment());
        if (dbView.getColumns() != null) {
            schemaIndex.setFields(dbView.getColumns().stream()
                    .map(SchemaIndex::ofDBTableColumn).collect(Collectors.toList()));
        }
        schemaIndex.setDefinition(dbView.getDdl());
        return schemaIndex;
    }

    public String getVectorIndexContent() {
        String comment = getComment();
        return getName() + (StringUtils.isBlank(comment) ? "" : ("-" + comment));
    }
}
