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

package com.oceanbase.odc.service.datasecurity.model;

import java.util.List;
import java.util.Map;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/9/7 14:57
 */
@Data
public class ListColumnsResp {

    private List<SchemaColumn> schemaColumns;

    public static ListColumnsResp of(List<SchemaColumn> schemaColumns) {
        ListColumnsResp resp = new ListColumnsResp();
        resp.setSchemaColumns(schemaColumns);
        return resp;
    }

    @Data
    public static class SchemaColumn {
        private String schemaName;
        private Map<String, List<DBTableColumn>> tableColumns;
        private Map<String, List<DBTableColumn>> viewColumns;
    }

}
