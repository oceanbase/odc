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

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryVirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.model.CommonVirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualElement;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The virtual element factory with {@code ResultSet} as the data source, constructs
 * {@code VirtualElement} according to the returned data of the database
 *
 * @author yh263208
 * @date 2021-11-03 17:10
 * @since ODC_release_3.2.2
 * @see VirtualElementFactory
 */
@Slf4j
public class ResultSetCachedElementFactory implements VirtualElementFactory {
    /**
     * {@code ResultSet} Note that this resultset cannot be iterated, otherwise it will cause other
     * modules that refer to this resultset to read incorrectly.
     */
    private final ResultSet resultSet;
    private final BinaryDataManager dataManager;
    private final ResultSetMetaData metaData;

    public ResultSetCachedElementFactory(@NonNull ResultSet resultSet, @NonNull BinaryDataManager dataManager)
            throws SQLException {
        this.resultSet = resultSet;
        this.dataManager = dataManager;
        this.metaData = resultSet.getMetaData();
    }

    @Override
    public VirtualElement generateElement(@NonNull String tableId, @NonNull Long rowId, @NonNull Integer columnId) {
        try {
            String dataType = this.metaData.getColumnTypeName(columnId + 1);
            String columnName = this.metaData.getColumnLabel(columnId + 1);
            if (DataTypeUtil.isBinaryType(dataType)) {
                InputStream inputStream = resultSet.getBinaryStream(columnId + 1);
                if (inputStream == null) {
                    return null;
                }
                BinaryContentMetaData metaData = this.dataManager.write(inputStream);
                return new BinaryVirtualElement(tableId, rowId, columnId, dataType, columnName, metaData);
            }
            Object value = resultSet.getObject(columnId + 1);
            if (value == null) {
                return null;
            }
            return new CommonVirtualElement(tableId, rowId, columnId, dataType, columnName, value);
        } catch (Exception e) {
            log.info("Failed to create element", e);
            throw new IllegalStateException(e);
        }
    }

}
