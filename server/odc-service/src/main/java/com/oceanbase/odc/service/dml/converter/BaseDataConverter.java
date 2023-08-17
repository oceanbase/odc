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

import java.util.Collection;

import com.oceanbase.odc.service.dml.DataConverter;
import com.oceanbase.odc.service.dml.DataValue;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link BaseDataConverter}
 *
 * @author yh263208
 * @date 2022-06-24 20:28
 * @since ODC_release_3.4.0
 */
public abstract class BaseDataConverter implements DataConverter {

    @Override
    public String convert(DataValue value) {
        if (value == null || value.getValue() == null) {
            return "NULL";
        }
        return doConvert(value);
    }

    protected boolean supports(@NonNull DataType dataType) {
        for (String dataTypeName : getSupportDataTypeNames()) {
            if (dataTypeName.equalsIgnoreCase(dataType.getDataTypeName())) {
                return true;
            }
        }
        return false;
    }

    protected abstract Collection<String> getSupportDataTypeNames();

    /**
     * Convert method for converter, convert string value for show to string for query eg. For timestamp
     * with time zone, the input string may 2021-06-03 18:12:21.00998 Asia/Shanghai We must convert it
     * to to_timestamp_tz('2021-06-03 18:12:21.00998 +08:00', 'YYYY-MM-DD HH24:MI:SS.FF TZH:TZM') for
     * query and insert
     *
     * @return convert string value
     */
    protected abstract String doConvert(@NonNull DataValue value);
}
