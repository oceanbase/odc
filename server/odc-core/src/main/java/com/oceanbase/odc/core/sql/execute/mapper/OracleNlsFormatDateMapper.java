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

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.core.sql.util.OracleDateFormat;

import lombok.NonNull;

/**
 * {@link OracleNlsFormatDateMapper}
 *
 * @author yh263208
 * @date 2023-07-04 21:01
 * @since ODC_release_4.2.0
 */
public class OracleNlsFormatDateMapper extends OracleGeneralDateMapper {

    private final OracleDateFormat format;

    public OracleNlsFormatDateMapper(@NonNull String pattern) {
        this.format = new OracleDateFormat(pattern, TimeZone.getDefault(), LocaleContextHolder.getLocale(), true);
    }

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        try {
            return map(data);
        } catch (Exception e) {
            Object obj = super.mapCell(data);
            return obj == null ? null : new TimeFormatResult(obj.toString());
        }
    }

    private Object map(CellData data) throws SQLException {
        Timestamp date = data.getTimestamp();
        if (date == null) {
            return null;
        }
        return new TimeFormatResult(format.format(date), date.getTime());
    }

}
