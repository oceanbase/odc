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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link JdbcColumnMapper} for data type
 *
 * <pre>
 *     {@code blob}
 *     {@code clob}
 *     {@code tinyblob}
 *     {@code longblob}
 *     {@code mediumblob}
 * </pre>
 */
public class GeneralLobMapper implements JdbcColumnMapper {

    private final static List<String> CANDIDATE_TYPES =
            Arrays.asList("BLOB", "CLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB");
    private final static int KB = 1024;
    private final static int MB = KB * 1024;
    private final static int GB = MB * 1024;

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException, IOException {
        InputStream inputStream = data.getBinaryStream();
        if (inputStream == null) {
            return null;
        }
        String unit = "B";
        int available = inputStream.available();
        if (available > GB) {
            available = available >> 30;
            unit = "GB";
        } else if (available > MB) {
            available = available >> 20;
            unit = "MB";
        } else if (available > KB) {
            available = available >> 10;
            unit = "KB";
        }
        return String.format("(%s) %d %s", data.getDataType().getDataTypeName(), available, unit);
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        for (String type : CANDIDATE_TYPES) {
            if (type.equalsIgnoreCase(dataType.getDataTypeName())) {
                return true;
            }
        }
        return false;
    }

}
