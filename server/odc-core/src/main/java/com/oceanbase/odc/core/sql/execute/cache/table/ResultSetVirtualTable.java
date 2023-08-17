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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.BiPredicate;

import com.oceanbase.odc.core.sql.execute.cache.VirtualElementFactory;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryVirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.model.CachedBinaryVirtualElement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@code VirtualTable} initialized with {@code Resultset}
 *
 * @author yh263208
 * @date 2021-11-04 12:13
 * @since ODC_release_3.2.2
 * @see CrossLinkedVirtualTable
 */
@Getter
public class ResultSetVirtualTable extends CrossLinkedVirtualTable {

    private final int maxCachedLines;
    private final long maxCachedSize;
    private boolean cacheFlag;
    private long totalCachedSize = 0;
    private long maxCachedRowId = 0;
    private int totalCachedLines = 0;
    @Getter(AccessLevel.NONE)
    private final BiPredicate<Integer, ResultSetMetaData> columnPredicate;

    /**
     * Default constructor, used to construct {@code VirtualTable}
     *
     * @param sqlId Id for a sql
     * @param columnPredicate Column predicate, the predicate determines which columns will be cached
     */
    public ResultSetVirtualTable(@NonNull String sqlId,
            @NonNull BiPredicate<Integer, ResultSetMetaData> columnPredicate) {
        this(sqlId, -1, -1, columnPredicate);
    }

    /**
     * Default constructor, used to construct {@code VirtualTable}
     *
     * @param sqlId Id for a sql
     * @param columnPredicate Column predicate, the predicate determines which columns will be cached
     */
    public ResultSetVirtualTable(@NonNull String sqlId,
            int maxCachedLines,
            long maxCachedSize,
            @NonNull BiPredicate<Integer, ResultSetMetaData> columnPredicate) {
        super(sqlId);
        this.columnPredicate = columnPredicate;
        this.maxCachedSize = maxCachedSize;
        this.maxCachedLines = maxCachedLines;
        refreshCacheFlag();
    }

    public synchronized VirtualLine addLine(@NonNull Long rowNum,
            @NonNull ResultSet resultSet,
            @NonNull VirtualElementFactory factory) throws SQLException {
        if (!cacheFlag) {
            return null;
        }
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            if (!columnPredicate.test(i, resultSetMetaData)) {
                continue;
            }
            VirtualElement element = factory.generateElement(tableId(), rowNum, i);
            if (element != null) {
                if (element instanceof CachedBinaryVirtualElement) {
                    totalCachedSize += ((CachedBinaryVirtualElement) element).getContent().getSizeInBytes();
                } else if (element instanceof BinaryVirtualElement) {
                    totalCachedSize += ((BinaryVirtualElement) element).getContent().getSizeInBytes();
                }
                put(element);
            }
        }
        LineNode lineNode = findLineNode(rowNum);
        if (lineNode == null) {
            return null;
        }
        totalCachedLines++;
        if (rowNum > maxCachedRowId) {
            maxCachedRowId = rowNum;
        }
        refreshCacheFlag();
        return new LinkedVirtualLine(lineNode);
    }

    private void refreshCacheFlag() {
        if (maxCachedLines >= 0 && totalCachedLines >= maxCachedLines) {
            cacheFlag = false;
            return;
        }
        cacheFlag = maxCachedSize < 0 || totalCachedSize < maxCachedSize;
    }

}
