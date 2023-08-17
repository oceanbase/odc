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

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.dml.DataValue;
import com.oceanbase.odc.service.dml.ValueContentType;
import com.oceanbase.odc.service.dml.ValueEncodeType;

import lombok.NonNull;

/**
 * {@link MySQLByteConverter}
 *
 * @author yh263208
 * @date 2022-06-26 16:29
 * @since ODC_release_3.4.0
 * @see BaseStringConverter
 */
public class MySQLByteConverter extends BaseStringConverter {

    @Override
    protected DialectType dialectType() {
        return DialectType.OB_MYSQL;
    }

    @Override
    protected Collection<String> getSupportDataTypeNames() {
        return Arrays.asList("tinyblob", "mediumblob", "longblob", "blob");
    }

    @Override
    protected String doConvert(@NonNull DataValue value) {
        if (value.getContentType() == ValueContentType.FILE) {
            return "load_file(" + super.doConvert(value) + ")";
        }
        byte[] content = value.getEncodeType().decode(value.getValue());
        return "unhex('" + ValueEncodeType.HEX.encodeToString(content).replaceAll(" ", "") + "')";
    }

}
