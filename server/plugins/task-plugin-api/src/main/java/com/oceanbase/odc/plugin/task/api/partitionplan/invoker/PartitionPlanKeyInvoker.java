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
package com.oceanbase.odc.plugin.task.api.partitionplan.invoker;

import java.sql.Connection;
import java.util.Map;

import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.NonNull;

/**
 * {@link PartitionPlanKeyInvoker}
 *
 * @author yh263208
 * @date 2024-01-12 15:02
 * @since ODC_release_4.2.4
 */
public interface PartitionPlanKeyInvoker<T> {
    /**
     * name of the {@link PartitionPlanKeyInvoker}
     *
     * @return get name
     */
    String getName();

    /**
     * invoke method
     *
     * @param connection the target {@link Connection}
     * @param dbTable target table
     * @param parameters parameters for this invoker
     */
    T invoke(@NonNull Connection connection, @NonNull DBTable dbTable, @NonNull Map<String, Object> parameters);

}
