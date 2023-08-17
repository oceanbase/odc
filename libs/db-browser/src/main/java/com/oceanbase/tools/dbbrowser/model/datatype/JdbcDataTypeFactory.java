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
package com.oceanbase.tools.dbbrowser.model.datatype;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import lombok.NonNull;

/**
 * {@link JdbcDataTypeFactory}
 *
 * @author yh263208
 * @date 2022-06-23 14:26
 * @since ODC_release_3.4.0
 */
public class JdbcDataTypeFactory implements DataTypeFactory {

    private final ResultSetMetaData metaData;
    private final int columnIndex;

    public JdbcDataTypeFactory(@NonNull ResultSetMetaData metaData, int columnIndex) {
        this.metaData = metaData;
        this.columnIndex = columnIndex;
    }

    /**
     * Get {@link DataType} from {@link ResultSetMetaData}
     *
     * @return {@link DataType}
     */
    @Override
    public DataType generate() throws SQLException {
        return new GeneralDataType(metaData, columnIndex);
    }
}
