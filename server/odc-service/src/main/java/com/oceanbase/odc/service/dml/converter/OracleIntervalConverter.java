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

import java.util.Arrays;
import java.util.Collection;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.dml.DataValue;

import lombok.NonNull;

/**
 * {@link OracleIntervalConverter}
 *
 * @author yh263208
 * @date 2022-06-26 16:28
 * @since ODC_release_3.4.0
 * @see BaseDataConverter
 */
public class OracleIntervalConverter extends BaseDataConverter {

    @Override
    protected Collection<String> getSupportDataTypeNames() {
        return Arrays.asList("interval day to second", "interval year to month");
    }

    @Override
    protected String doConvert(@NonNull DataValue value) {
        return "'" + StringUtils.escapeUseDouble(value.getValue(), (char) 39) + "'";
    }

}
