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
package com.oceanbase.odc.core.sql.execute.cache.table;

/**
 * Virtual table event listener, used to monitor some events on the {@code VirtualTable}
 *
 * @author yh263208
 * @date 2021-11-03 15:41
 * @since ODC_release_3.2.2
 */
public interface VirtualTableEventListener {
    /**
     * This method is called when the element is placed to notify the listener
     *
     * @param virtualTable {@code VirtualTable}
     * @param element Placed element
     */
    void onElementPut(VirtualTable virtualTable, VirtualElement element);

    /**
     * This method is called when a virtual column is added
     *
     * @param virtualTable {@code VirtualTable}
     * @param virtualColumn Placed column {@code VirtualColumn}
     */
    void onColumnAdded(VirtualTable virtualTable, VirtualColumn virtualColumn);

    /**
     * This method is called when a virtual line is added
     *
     * @param virtualTable {@code VirtualTable}
     * @param virtualLine Placed line {@code VirtualLine}
     */
    void onLineAdded(VirtualTable virtualTable, VirtualLine virtualLine);

}

