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

import java.util.List;

import com.oceanbase.odc.core.flow.model.AbstractFlowTaskResult;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskEntity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author jingtian
 * @date 2024/1/3
 * @since ODC_release_4.2.4
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DBStructureComparisonTaskResult extends AbstractFlowTaskResult {
    /**
     * Refer to {@link StructureComparisonTaskEntity#getId()}
     */
    private Long taskId;
    private TaskStatus status;
    private List<Comparing> comparingList;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Comparing {
        private DBObjectType dbObjectType;
        private String dbObjectName;
    }

    public DBStructureComparisonTaskResult(TaskStatus status) {
        this.status = status;
    }
}
