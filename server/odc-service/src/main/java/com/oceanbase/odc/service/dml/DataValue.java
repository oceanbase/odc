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

import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.Getter;
import lombok.NonNull;

/**
 * {@link DataValue}
 *
 * @author yh263208
 * @date 2022-05-04 21:55
 * @since ODC_release_3.3.1
 */
@Getter
public class DataValue {

    private final String value;
    private final ValueContentType contentType;
    private final ValueEncodeType encodeType;
    private final DataType dataType;

    public static DataValue ofRawValue(String rawValue, @NonNull String dataType) {
        return new DataValue(rawValue, dataType, ValueContentType.RAW, ValueEncodeType.TXT);
    }

    public DataValue(String value, @NonNull String dataType, @NonNull ValueContentType contentType,
            @NonNull ValueEncodeType encodeType) {
        this.value = value;
        this.contentType = contentType;
        this.encodeType = encodeType;
        CommonDataTypeFactory factory = new CommonDataTypeFactory(dataType);
        this.dataType = factory.generate();
    }
}
