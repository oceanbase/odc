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

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;

import lombok.Data;

/**
 * @author jingtian
 * @date 2023/12/29
 * @since ODC_release_4.2.4
 */
@Data
public class DBStructureComparisonParameter implements Serializable, TaskParameters {
    /**
     * Source database id, refer to {@link DatabaseEntity#getId()}
     */
    @NotNull
    private Long sourceDatabaseId;
    /**
     * Target database id, refer to {@link DatabaseEntity#getId()}
     */
    @NotNull
    private Long targetDatabaseId;
    @NotNull
    private ComparisonScope comparisonScope;
    private List<String> tableNamesToBeCompared;

    private enum ComparisonScope {
        ALL,
        PART
    }
}
