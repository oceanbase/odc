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
package com.oceanbase.odc.service.rollbackplan.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author jingtian
 * @date 2023/5/11
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class TableReferenece {
    private String schemaName;
    private String tableName;
    private String alias;

    public TableReferenece(String schemaName, String tableName, String alias) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.alias = alias;
    }

}
