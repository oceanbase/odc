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
package com.oceanbase.odc.metadb.structurecompare;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/1/29
 * @since ODC_release_4.2.4
 */
public class StructureComparisonEntitySpecs {
    private static final String STRUCTURE_COMPARISON_TASK_ID = "comparisonTaskId";
    private static final String COMPARING_RESULT = "comparingResult";
    private static final String DB_OBJECT_NAME = "databaseObjectName";

    public static Specification<StructureComparisonEntity> comparisonTaskIdEquals(@NonNull Long comparisonTaskId) {
        return (root, query, builder) -> builder.equal(root.get(STRUCTURE_COMPARISON_TASK_ID), comparisonTaskId);
    }

    public static Specification<StructureComparisonEntity> comparisonResultEquals(
            @NonNull ComparisonResult comparisonResult) {
        return (root, query, builder) -> builder.equal(root.get(COMPARING_RESULT), comparisonResult);
    }

    public static Specification<StructureComparisonEntity> dbObjectNameLike(
            @NonNull String dbObjectName) {
        return (root, query, builder) -> builder.like(root.get(DB_OBJECT_NAME),
                "%" + StringUtils.escapeLike(dbObjectName) + "%");
    }
}
