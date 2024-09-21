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
package com.oceanbase.odc.service.loaddata.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xien.sxe
 * @date 2024/3/11
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class ColumnPreview implements Serializable {

    /**
     * 用于序列化的版本UID。
     */
    private static final long serialVersionUID = 8449864678080877932L;

    /**
     * 列名。
     */
    private String name;

    /**
     * 列的数据类型，以字符串形式表示。
     */
    private String dataType;

    /**
     * 内部使用的数据类型与精度。在JSON序列化时被忽略。
     */
    @JsonIgnore
    private DataTypeWithPrecision internalDataType;

    /**
     * 标识该列的主键顺序。0 表示非主键列
     */
    private int primaryOrdinal;

    /**
     * 标识该列的值是否不允许为空。
     */
    private boolean notNull;

    /**
     * 该列的第一个值，用于预览。
     */
    private String firstColumnValue;

    /**
     * 该列在目标表中的位置。
     */
    private int destColumnPosition;

    public ColumnPreview(String name) {
        this.name = name;
    }

    public ColumnPreview(String name, DataTypeWithPrecision dataTypeWithPrecision, int primaryOrdinal, boolean notNull,
            String firstColumnValue) {
        this.name = name;
        this.internalDataType = dataTypeWithPrecision;
        this.primaryOrdinal = primaryOrdinal;
        this.notNull = notNull;
        this.firstColumnValue = firstColumnValue;
    }

    // for front-end show
    public String getDataType() {
        return internalDataType == null ? dataType : internalDataType.toString();
    }
}
