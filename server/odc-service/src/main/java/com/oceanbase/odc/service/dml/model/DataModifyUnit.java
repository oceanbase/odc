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
package com.oceanbase.odc.service.dml.model;

import javax.validation.constraints.Size;

import com.oceanbase.odc.service.dml.DataValue;
import com.oceanbase.odc.service.dml.ValueContentType;
import com.oceanbase.odc.service.dml.ValueEncodeType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Data Modify unit object
 *
 * @author yh263208
 * @date 2021-06-10 12:25
 * @since ODC_release_2.4.2
 */
@Getter
@Setter
@ToString(exclude = {"oldData", "newData"})
public class DataModifyUnit {
    private String schemaName;
    private String tableName;
    private String columnName;
    private String columnType;
    private String oldData;
    // 最大支持 200KB 数据编辑
    @Size(max = 200 * 1024, message = "The length of the field exceeds the maximum limit: [0, 200 * 1024]")
    private String newData;
    private String newDataType = "RAW";
    private boolean queryColumn;
    /**
     * use DEFAULT while insert/update, skip newData
     */
    private boolean useDefault = false;

    public DataValue getNewDataValue() {
        ValueContentType contentType = ValueContentType.RAW;
        if ("FILE".equalsIgnoreCase(newDataType)) {
            contentType = ValueContentType.FILE;
        }
        ValueEncodeType encodeType = ValueEncodeType.TXT;
        if ("HEX".equalsIgnoreCase(newDataType)) {
            encodeType = ValueEncodeType.HEX;
        }
        return new DataValue(newData, columnType, contentType, encodeType);
    }

}
