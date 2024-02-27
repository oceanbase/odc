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

import java.sql.Timestamp;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

public class TimestampCellData extends TestCellData {

    private final Timestamp timestamp;

    public TimestampCellData(Timestamp timestamp, @NonNull DataType dataType) {
        super(dataType);
        this.timestamp = timestamp;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public byte[] getBytes() {
        if (timestamp == null) {
            return null;
        }
        String returnValue = timestamp.toString();
        if (returnValue.endsWith(".0")) {
            return returnValue.substring(0, returnValue.length() - 2).getBytes();
        }
        return returnValue.getBytes();
    }

    public String getString() {
        return null;
    }

}
