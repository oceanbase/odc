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
package com.oceanbase.odc.service.dml.converter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.dml.DataConverter;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link DataConverters}
 *
 * @author yh263208
 * @date 2022-06-26 16:36
 * @since ODC_release_3.4.0
 */
public class DataConverters {

    private final List<BaseDataConverter> converterList;

    private DataConverters(DialectType dialectType, String serverTimeZoneId) {
        converterList = new LinkedList<>();
        if (DialectType.OB_ORACLE.equals(dialectType)) {
            initForOracleMode(serverTimeZoneId);
        } else if (Objects.nonNull(dialectType) && dialectType.isMysql()) {
            initForMysqlMode();
        } else {
            throw new IllegalArgumentException("Illegal DialectType " + dialectType);
        }
    }

    private void initForMysqlMode() {
        converterList.add(new MySQLStringConverter());
        converterList.add(new MySQLByteConverter());
        converterList.add(new MySQLBinaryConverter());
        converterList.add(new MySQLDateAndTimeConverter());
        converterList.add(new MySQLBitConverter());
        converterList.add(new MySQLEnumConverter());
        converterList.add(new MySQLSetConverter());
    }

    private void initForOracleMode(String serverTimeZoneId) {
        converterList.add(new OracleStringConverter());
        converterList.add(new OracleTimeStampTZConverter());
        converterList.add(new OracleTimeStampConverter(serverTimeZoneId));
        converterList.add(new OracleDateConverter());
        converterList.add(new OracleClobConverter());
        converterList.add(new OracleByteConverter());
        converterList.add(new OracleRowIDConverter());
        converterList.add(new OracleIntervalConverter());
    }

    public DataConverter get(@NonNull DataType dataType) {
        for (BaseDataConverter converter : converterList) {
            if (converter.supports(dataType)) {
                return converter;
            }
        }
        return null;
    }

    public static DataConverters getConvertersByDialectType(DialectType dialectType, String serverTimeZoneId) {
        return new DataConverters(dialectType, serverTimeZoneId);
    }

}
