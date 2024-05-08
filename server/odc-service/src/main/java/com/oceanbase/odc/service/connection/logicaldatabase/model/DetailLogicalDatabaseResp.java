/*
 * Copyright (c) 2024 OceanBase.
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

import java.util.List;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/5/7 16:44
 * @Description: []
 */
@Data
public class DetailLogicalDatabaseResp {
    private Long id;

    private String name;

    private String alias;

    private DialectType dialectType;

    private Environment environment;

    private List<Database> physicalDatabases;

    private List<DetailLogicalTableResp> logicalTables;
}
