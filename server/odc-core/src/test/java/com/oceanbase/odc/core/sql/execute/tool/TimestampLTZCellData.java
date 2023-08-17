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
package com.oceanbase.odc.core.sql.execute.tool;

import com.oceanbase.jdbc.extend.datatype.TIMESTAMPLTZ;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

public class TimestampLTZCellData extends TestCellData {

    private final TIMESTAMPLTZ timestamp;

    public TimestampLTZCellData(TIMESTAMPLTZ timestamp, @NonNull DataType dataType) {
        super(dataType);
        this.timestamp = timestamp;
    }

    public Object getObject() {
        return timestamp;
    }

}
