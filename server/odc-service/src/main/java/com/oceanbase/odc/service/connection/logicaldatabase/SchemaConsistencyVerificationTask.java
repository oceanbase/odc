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

package com.oceanbase.odc.service.connection.logicaldatabase;

import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTableEntity;

/**
 * @Author: Lebie
 * @Date: 2024/5/10 18:57
 * @Description: []
 */
public class SchemaConsistencyVerificationTask implements Runnable {
    private final LogicalTableEntity logicalTable;
    public SchemaConsistencyVerificationTask(LogicalTableEntity logicalTable) {
        this.logicalTable = logicalTable;
    }

    @Override
    public void run() {

    }
}
