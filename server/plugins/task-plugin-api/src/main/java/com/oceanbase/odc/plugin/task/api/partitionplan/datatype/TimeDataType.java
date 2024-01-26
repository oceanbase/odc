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
package com.oceanbase.odc.plugin.task.api.partitionplan.datatype;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.GeneralDataType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link TimeDataType}
 *
 * @author yh263208
 * @date 2024-01-09 15:38
 * @since ODC_release_4.2.4
 * @see DataType
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class TimeDataType extends GeneralDataType {

    public static final int YEAR = 0x1;
    public static final int MONTH = 0x2 | YEAR;
    public static final int DAY = 0x4 | MONTH;
    public static final int HOUR = 0x8 | DAY;
    public static final int MINUTE = 0x10 | HOUR;
    public static final int SECOND = 0x20 | MINUTE;
    private final String realColumnTypeName;

    public TimeDataType(@NonNull String realColumnTypeName, @NonNull int precision) {
        super(precision, -1, "TIME");
        this.realColumnTypeName = realColumnTypeName;
    }

}
