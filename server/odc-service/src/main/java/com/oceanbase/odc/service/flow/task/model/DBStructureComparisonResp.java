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

import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskResultEntity;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jingtian
 * @date 2024/1/3
 * @since ODC_release_4.2.4
 */
@Data
@NoArgsConstructor
public class DBStructureComparisonResp {
    /**
     * Refer to structure_comparison_task.id
     */
    private Long id;
    private PaginatedResponse<ObjectComparisonResult> comparisonResults;
    private String totalChangeScript;
    private String storageObjectId;
    private boolean isOverSizeLimit;

    @Data
    @NoArgsConstructor
    public static class ObjectComparisonResult {
        /**
         * Refer to {@link DBObjectStructureComparisonResp#getId()}
         */
        private Long structureComparisonId;
        private DBObjectType dbObjectType;
        private String dbObjectName;
        private OperationType operationType;

        public static ObjectComparisonResult fromEntity(StructureComparisonTaskResultEntity entity) {
            ObjectComparisonResult returnVal = new ObjectComparisonResult();
            returnVal.setStructureComparisonId(entity.getId());
            returnVal.setDbObjectName(entity.getDatabaseObjectName());
            returnVal.setDbObjectType(entity.getDatabaseObjectType());
            returnVal.setOperationType(OperationType.fromComparisonResult(entity.getComparingResult()));
            return returnVal;

        }
    }
    public enum OperationType {
        CREATE(ComparisonResult.ONLY_IN_SOURCE),
        UPDATE(ComparisonResult.INCONSISTENT),
        DROP(ComparisonResult.ONLY_IN_TARGET),
        NO_ACTION(ComparisonResult.CONSISTENT),
        SKIP(ComparisonResult.MISSING_IN_SOURCE),
        UNSUPPORTED(ComparisonResult.UNSUPPORTED);

        private ComparisonResult comparisonResult;

        OperationType(ComparisonResult comparisonResult) {
            this.comparisonResult = comparisonResult;
        }

        public ComparisonResult getComparisonResult() {
            return comparisonResult;
        }

        public static OperationType fromComparisonResult(ComparisonResult comparisonResult) {
            switch (comparisonResult) {
                case ONLY_IN_SOURCE:
                    return CREATE;
                case INCONSISTENT:
                    return UPDATE;
                case ONLY_IN_TARGET:
                    return DROP;
                case CONSISTENT:
                    return NO_ACTION;
                case MISSING_IN_SOURCE:
                    return SKIP;
                case UNSUPPORTED:
                    return UNSUPPORTED;
                default:
                    throw new IllegalArgumentException("Unknown comparison result: " + comparisonResult);
            }
        }
    }
}
