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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.NonNull;

/**
 * {@link DefaultJdbcRowMapper}
 *
 * @author yh263208
 * @date 2022-06-13 16:20
 * @since ODC_release_3.4.0
 * @see BaseDialectBasedRowMapper
 */
public class DefaultJdbcRowMapper extends BaseDialectBasedRowMapper {

    private final List<JdbcColumnMapper> mapperList = new LinkedList<>();

    public DefaultJdbcRowMapper(@NonNull ConnectionSession session) {
        super(session.getDialectType());
        DialectType dialectType = session.getDialectType();
        if (Objects.nonNull(dialectType) && dialectType.isOBMysql()) {
            mapperList.add(new MySQLBitMapper());
            mapperList.add(new MySQLDatetimeMapper());
            mapperList.add(new MySQLYearMapper());
            mapperList.add(new MySQLTimestampMapper());
        } else if (dialectType == DialectType.OB_ORACLE) {
            mapperList.add(new OracleNlsFormatDateMapper(
                    ConnectionSessionUtil.getNlsDateFormat(session)));
            mapperList.add(new OracleNlsFormatTimestampTZMapper(
                    ConnectionSessionUtil.getNlsTimestampTZFormat(session)));
            mapperList.add(new OracleNlsFormatTimestampLTZMapper(
                    ConnectionSessionUtil.getNlsTimestampTZFormat(session),
                    ConnectionSessionUtil.getConsoleSessionTimeZone(session)));
            mapperList.add(new OracleIntervalMapper());
            mapperList.add(new OracleNlsFormatTimestampMapper(
                    ConnectionSessionUtil.getNlsTimestampFormat(session)));
            mapperList.add(new OracleNumberMapper());
            mapperList.add(new OracleBinaryNumberMapper());
        }
        mapperList.add(new GeneralLobMapper());
    }

    @Override
    protected Collection<JdbcColumnMapper> getColumnDataMappers(@NonNull DialectType dialectType) {
        return mapperList;
    }

}

