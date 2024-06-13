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
package com.oceanbase.odc.service.connection.logicaldatabase.model;

import java.util.Date;
import java.util.List;

import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/5/7 19:39
 * @Description: []
 */

@Data
public class DetailLogicalTableResp {
    private Long id;

    private String name;

    private String expression;

    private Integer physicalTableCount;

    private List<DataNode> inconsistentPhysicalTables;

    private DBTable basePhysicalTable;
}
