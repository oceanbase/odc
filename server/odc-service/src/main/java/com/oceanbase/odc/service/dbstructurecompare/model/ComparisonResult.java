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

package com.oceanbase.odc.service.dbstructurecompare.model;

import com.oceanbase.odc.service.dbstructurecompare.model.SchemaStructureComparisonResult.OperationType;

import lombok.Getter;

/**
 * @author jingtian
 * @date 2023/12/19
 * @since ODC_release_4.2.4
 */
public enum ComparisonResult {
    ONLY_IN_SOURCE(OperationType.CREATE),
    ONLY_IN_TARGET(OperationType.DROP),
    CONSISTENT(OperationType.NO_ACTION),
    INCONSISTENT(OperationType.UPDATE),
    MISSING_IN_SOURCE(OperationType.SKIP),
    UNSUPPORTED(OperationType.UNSUPPORTED);

    @Getter
    private OperationType operationType;

    private ComparisonResult(OperationType operationType) {
        this.operationType = operationType;
    }
}
