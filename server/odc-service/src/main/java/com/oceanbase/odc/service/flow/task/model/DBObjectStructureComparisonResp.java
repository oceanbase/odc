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
package com.oceanbase.odc.service.flow.task.model;

import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp.OperationType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;

/**
 * @author jingtian
 * @date 2024/1/3
 * @since ODC_release_4.2.4
 */
@Data
public class DBObjectStructureComparisonResp {
    /**
     * Refer to structure_comparison.id
     */
    private Long id;
    private DBObjectType dbObjectType;
    private String dbObjectName;
    private OperationType operationType;
    private String sourceObjectDdl;
    private String targetObjectDdl;
    private String changeScript;
}
