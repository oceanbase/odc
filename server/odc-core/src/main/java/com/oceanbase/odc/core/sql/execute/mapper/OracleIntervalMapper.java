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
package com.oceanbase.odc.core.sql.execute.mapper;

import java.sql.SQLException;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link JdbcColumnMapper} for data type {@code interval day to second} and
 * {@code interval year to month}
 *
 * @author yh263208
 * @date 2022-06-28 18:21
 * @since ODC_release_3.4.0
 * @see JdbcColumnMapper
 */
public class OracleIntervalMapper implements JdbcColumnMapper {

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        String obj = data.getString();
        if (obj == null) {
            return null;
        }
        if (obj.startsWith("-")) {
            return obj;
        }
        return "+" + obj;
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return "INTERVALDS".equalsIgnoreCase(dataType.getDataTypeName())
                || "INTERVALYM".equalsIgnoreCase(dataType.getDataTypeName());
    }
}
