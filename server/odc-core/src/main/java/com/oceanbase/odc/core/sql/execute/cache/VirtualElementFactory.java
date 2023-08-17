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
package com.oceanbase.odc.core.sql.execute.cache;

import com.oceanbase.odc.core.sql.execute.cache.table.VirtualElement;

import lombok.NonNull;

/**
 * Virtual element factory class, used to generate {@code VirutalElement}
 *
 * @author yh263208
 * @date 2021-11-03 17:06
 * @since ODC_release_3.2.2
 */
public interface VirtualElementFactory {
    /**
     * Call this method to get a virtual element
     *
     * @param tableId id for a table
     * @param rowId id for a row
     * @param columnId id for a column
     * @return {@code VirtualElement}
     */
    VirtualElement generateElement(@NonNull String tableId, @NonNull Long rowId, @NonNull Integer columnId);

}

