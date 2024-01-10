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

package com.oceanbase.odc.service.structurecompare.model;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;

/**
 * @author jingtian
 * @date 2023/12/8
 * @since ODC_release_4.2.4
 */
@Data
public class DBObjectComparisonResult {
    private DBObjectType dbObjectType;
    private String dbObjectName;
    private String sourceSchemaName;
    private String targetSchemaName;
    private String sourceDdl;
    private String targetDdl;
    private ComparisonResult comparisonResult;
    /**
     * For TABLE object, such as COLUMN, INDEX, CONSTRAINT and PARTITION when comparisonResult is
     * UPDATE.
     */
    private List<DBObjectComparisonResult> subDBObjectComparisonResult = new ArrayList<>();
    private String changeScript;

    public DBObjectComparisonResult(DBObjectType dbObjectType, String dbObjectName, String sourceSchemaName,
            String targetSchemaName) {
        this.dbObjectType = dbObjectType;
        this.dbObjectName = dbObjectName;
        this.sourceSchemaName = sourceSchemaName;
        this.targetSchemaName = targetSchemaName;
    }

    public DBObjectComparisonResult(DBObjectType dbObjectType, String sourceSchemaName, String targetSchemaName) {
        this.dbObjectType = dbObjectType;
        this.sourceSchemaName = sourceSchemaName;
        this.targetSchemaName = targetSchemaName;
    }
}
