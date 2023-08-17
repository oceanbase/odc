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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.NonNull;

/**
 * Virtual table, ODC needs to store some SQL query results in the local cache due to the need to
 * solve the problem of viewing large fields. The virtual table is abstracted for the execution
 * results of a certain SQL, and some basic query operations can be implemented on it.
 *
 * @author yh263208
 * @date 2021-11-02 16:21
 * @since ODC_release_3.2.2
 */
public interface VirtualTable {
    /**
     * Corresponding to the projection concept in the database table, project a certain column or
     * certain columns in a table, and the return result is another subset of the virtual table
     *
     * @param columnIds Column id collection
     * @param columenMapper Column mapping function, used to map one column to another
     * @throws NullPointerException The column id may not exist, and a null pointer is thrown at this
     *         time
     * @return {@code VirtualTable}
     */
    VirtualTable project(@NonNull List<Integer> columnIds,
            @NonNull Function<VirtualColumn, VirtualColumn> columenMapper) throws NullPointerException;

    /**
     * Corresponding to the selection operation of the database table, a predicate is passed in to
     * determine whether a row will be in the selection result
     *
     * @param predicate {@code Predicate}
     * @return {@code VirtualTable}
     */
    VirtualTable select(@NonNull Predicate<VirtualLine> predicate);

    /**
     * Id of this {@code VirtualTable}
     *
     * @return ID
     */
    String tableId();

    /**
     * Get table size
     *
     * @return count of a {@code VirtualTable}
     */
    Long count();

    /**
     * Get column id list
     */
    List<Integer> columnIds();

    /**
     * Traverse each row of the virtual table
     *
     * @param lineConsumer current line
     */
    void forEach(@NonNull Consumer<VirtualLine> lineConsumer);

}
