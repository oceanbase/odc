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
package com.oceanbase.odc.service.rollbackplan;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.mapper.BaseDialectBasedRowMapper;
import com.oceanbase.odc.core.sql.execute.mapper.GeneralLobMapper;
import com.oceanbase.odc.core.sql.execute.mapper.JdbcColumnMapper;
import com.oceanbase.odc.core.sql.execute.mapper.MySQLBitMapper;
import com.oceanbase.odc.core.sql.execute.mapper.MySQLDatetimeMapper;
import com.oceanbase.odc.core.sql.execute.mapper.MySQLGeometryMapper;
import com.oceanbase.odc.core.sql.execute.mapper.MySQLTimestampMapper;
import com.oceanbase.odc.core.sql.execute.mapper.MySQLYearMapper;
import com.oceanbase.odc.core.sql.execute.mapper.OracleGeneralDateMapper;
import com.oceanbase.odc.core.sql.execute.mapper.OracleGeneralTimestampLTZMapper;
import com.oceanbase.odc.core.sql.execute.mapper.OracleGeneralTimestampMapper;
import com.oceanbase.odc.core.sql.execute.mapper.OracleGeneralTimestampTZMapper;
import com.oceanbase.odc.core.sql.execute.mapper.OracleIntervalMapper;

import lombok.NonNull;

public class RollBackPlanJdbcRowMapper extends BaseDialectBasedRowMapper {

    private final List<JdbcColumnMapper> mapperList = new LinkedList<>();

    public RollBackPlanJdbcRowMapper(@NonNull DialectType dialectType, String timeZone) {
        super(dialectType);
        if (dialectType.isMysql()) {
            mapperList.add(new MySQLBitMapper());
            mapperList.add(new MySQLDatetimeMapper());
            mapperList.add(new MySQLYearMapper());
            mapperList.add(new MySQLTimestampMapper());
            mapperList.add(new MySQLGeometryMapper());
        } else if (dialectType == DialectType.OB_ORACLE) {
            mapperList.add(new OracleGeneralDateMapper());
            mapperList.add(new OracleGeneralTimestampTZMapper());
            mapperList.add(new OracleGeneralTimestampLTZMapper(timeZone));
            mapperList.add(new OracleIntervalMapper());
            mapperList.add(new OracleGeneralTimestampMapper());
        }
        mapperList.add(new GeneralLobMapper());
    }

    @Override
    protected Collection<JdbcColumnMapper> getColumnDataMappers(@NonNull DialectType dialectType) {
        return mapperList;
    }

}
