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

import com.oceanbase.jdbc.extend.datatype.TIMESTAMPLTZ;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link JdbcColumnMapper} for data type {@code timestamp[0-9] with local time zone}
 *
 * @author yh263208
 * @date 2022-06-28 18:00
 * @since ODC_release_3.4.0
 * @see OracleGeneralTimestampTZMapper
 */
public class OracleGeneralTimestampLTZMapper extends OracleGeneralTimestampTZMapper {

    protected final String serverTimeZoneStr;

    public OracleGeneralTimestampLTZMapper(String serverTimeZoneStr) {
        this.serverTimeZoneStr = serverTimeZoneStr;
    }

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        Object obj = data.getObject();
        if (obj == null) {
            return null;
        }
        Verify.notNull(serverTimeZoneStr, "ServerTimeZone");
        TIMESTAMPLTZ timestampltz = (TIMESTAMPLTZ) obj;
        return toString(timestampltz.toBytes(), serverTimeZoneStr, true);
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return "TIMESTAMP WITH LOCAL TIME ZONE".equalsIgnoreCase(dataType.getDataTypeName());
    }

}

