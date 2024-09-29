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

import lombok.Getter;

/**
 * @author xien.sxe
 * @date 2024/2/29
 * @since 1.0.0
 */
public enum DataType {

    // common
    DATE(false, false),
    TIMESTAMP(false, true),
    BLOB(false, false),
    CLOB(false, false),
    CHAR(true, false),

    // oracle
    NUMBER(true, true),
    VARCHAR2(true, false),

    // mysql
    VARCHAR(true, false),
    TINYINT(true, false),
    SMALLINT(true, false),
    BIGINT(true, false),
    INT(true, false),
    FLOAT(false, false),
    DOUBLE(false, false),
    MEDIUMTEXT(false, false),
    DECIMAL(true, true),
    TIME(false, true),
    DATETIME(false, true),
    YEAR(false, false),
    JSON(false, false);

    @Getter
    private final boolean supportLength;

    @Getter
    private final boolean supportPrecision;

    DataType(boolean supportLength, boolean supportPrecision) {
        this.supportLength = supportLength;
        this.supportPrecision = supportPrecision;
    }
}
