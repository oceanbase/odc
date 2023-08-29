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

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.dml.DataValue;
import com.oceanbase.odc.service.dml.ValueEncodeType;

import lombok.NonNull;

/**
 * {@link BaseStringConverter}
 *
 * @author yh263208
 * @date 2022-06-26 16:23
 * @since ODC_release_3.4.0
 * @see com.oceanbase.odc.service.dml.converter.BaseDataConverter
 */
public abstract class BaseStringConverter extends BaseDataConverter {

    @Override
    protected String doConvert(@NonNull DataValue dataValue) {
        ValueEncodeType encodeType = dataValue.getEncodeType();
        String value = new String(encodeType.decode(dataValue.getValue()), StandardCharsets.UTF_8);
        if (Objects.nonNull(dialectType()) && dialectType().isMysql()) {
            value = StringUtils.escapeUseDouble(value, (char) 92);
        }
        return "'" + StringUtils.escapeUseDouble(value, (char) 39) + "'";
    }

    protected abstract DialectType dialectType();
}
