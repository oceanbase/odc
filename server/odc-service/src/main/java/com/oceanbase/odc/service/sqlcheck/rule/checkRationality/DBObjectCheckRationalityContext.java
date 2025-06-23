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
package com.oceanbase.odc.service.sqlcheck.rule.checkRationality;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/6/18 20:13
 * @since: 4.3.4
 */
@Data
public class DBObjectCheckRationalityContext {

    private final String defaultSchema;

    private final Map<String, Boolean> tableExistMap;

    private final Map<String, Boolean> externalTableExistMap;

    private final Map<String, Boolean> viewExistMap;

    private final Map<String, Boolean> materializedViewExistMap;

    private final Map<String, Boolean> indexExistMap;

    private final Map<String, Boolean> columnExistMap;

    public DBObjectCheckRationalityContext(String defaultSchema) {
        this.defaultSchema = defaultSchema;
        this.tableExistMap = new HashMap<>();
        this.externalTableExistMap = new HashMap<>();
        this.viewExistMap = new HashMap<>();
        this.materializedViewExistMap = new HashMap<>();
        this.indexExistMap = new HashMap<>();
        this.columnExistMap = new HashMap<>();
    }

}
