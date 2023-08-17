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
 * {@code VirtualElement}, the smallest unit stored in the query cache is the element. The virtual
 * element is the upper abstraction for the underlying storage, and records some meta-information of
 * the element
 *
 * @author yh263208
 * @date 2021-11-02 19:25
 * @since ODC_release_3.2.2
 */
public interface VirtualElement {
    /**
     * The id of the table
     *
     * @return ID of the table
     */
    String tableId();

    /**
     * The id of the column where the virtual element is located
     *
     * @return ID of the column
     */
    Integer columnId();

    /**
     * Id of the row where the virtual element is located
     *
     * @return ID of the row
     */
    Long rowId();

    /**
     * Element data type
     *
     * @return data type of the elt
     */
    String dataTypeName();

    /**
     * Name of the column
     *
     * @return Name of the column
     */
    String columnName();

    /**
     * Get the actual content of the element
     *
     * @return content of the element
     */
    Object getContent();

}

