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
package com.oceanbase.odc.service.dml;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.dml.converter.DataConverters;

import lombok.NonNull;

/**
 * {@link DataConvertUtil}
 *
 * @author yh263208
 * @date 2022-06-26 16:40
 * @since ODC_release_3.4.0
 */
public class DataConvertUtil {

    @Deprecated
    public static String convertToSqlString(@NonNull DialectType dialectType, DataValue dataValue) {
        return convertToSqlString(dialectType, dataValue, null);
    }

    public static String convertToSqlString(@NonNull DialectType dialectType,
            DataValue dataValue, String serverTimeZoneId) {
        if (dataValue == null) {
            return "NULL";
        }
        DataConverter converter = DataConverters
                .getConvertersByDialectType(dialectType, serverTimeZoneId)
                .get(dataValue.getDataType());
        if (converter == null) {
            return dataValue.getValue();
        }
        return converter.convert(dataValue);
    }

    public static String convertToSqlString(@NonNull ConnectionSession session, DataValue dataValue) {
        return convertToSqlString(session.getDialectType(), dataValue,
                ConnectionSessionUtil.getConsoleSessionTimeZone(session));
    }

    public static String convertToSqlString(@NonNull DialectType dialectType,
            @NonNull String dataType, String value) {
        return convertToSqlString(dialectType, DataValue.ofRawValue(value, dataType));
    }

}
