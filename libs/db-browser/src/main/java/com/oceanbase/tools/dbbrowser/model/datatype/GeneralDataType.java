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

import org.apache.commons.lang3.Validate;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * {@link GeneralDataType}
 *
 * @author yh263208
 * @date 2022-06-22 17:09
 * @since ODC_release_3.4.0
 * @see DataType
 */
@EqualsAndHashCode
public class GeneralDataType implements DataType {

    private final int precision;
    private final int scale;
    private final String columnTypeName;

    public GeneralDataType(int precision, int scale, @NonNull String dataTypeName) {
        this.precision = precision;
        this.scale = scale;
        this.columnTypeName = dataTypeName;
    }

    public GeneralDataType(int precision, String dataTypeName) {
        this(precision, 0, dataTypeName);
    }

    public GeneralDataType(String dataTypeName) {
        this(0, 0, dataTypeName);
    }

    public GeneralDataType(@NonNull ResultSetMetaData metaData, int columnIndex) throws SQLException {
        Validate.isTrue(columnIndex >= 0, "ColumnIndex can not be negative");
        this.precision = metaData.getPrecision(columnIndex + 1);
        this.scale = metaData.getScale(columnIndex + 1);
        this.columnTypeName = metaData.getColumnTypeName(columnIndex + 1);
    }

    @Override
    public Integer getScale() {
        return this.scale;
    }

    @Override
    public Integer getPrecision() {
        return this.precision;
    }

    @Override
    public String getDataTypeName() {
        return this.columnTypeName;
    }

}
