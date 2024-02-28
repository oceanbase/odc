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
import java.sql.Timestamp;
import java.util.TimeZone;

import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OracleNlsFormatTimestampLTZMapper}
 *
 * @author jingtian
 * @date 2024/2/21
 * @since ODC_release_4.2.4
 */
public class OracleNlsFormatTimestampLTZMapper extends OracleNlsFormatTimestampTZMapper {

    public OracleNlsFormatTimestampLTZMapper(@NonNull String pattern) {
        super(pattern);
    }

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        Timestamp timestamp = data.getTimestamp();
        if (timestamp == null) {
            return null;
        }
        return new TimeFormatResult(this.format.format(timestamp), timestamp.getTime(), timestamp.getNanos(),
                TimeZone.getDefault().getID());
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return "TIMESTAMP WITH LOCAL TIME ZONE".equalsIgnoreCase(dataType.getDataTypeName());
    }
}
