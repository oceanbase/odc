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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

public class DateCellData extends TestCellData {

    private final Date date;

    public DateCellData(Date date, @NonNull DataType dataType) {
        super(dataType);
        this.date = date;
    }

    public byte[] getBytes() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return (calendar.get(Calendar.YEAR) + "").getBytes();
    }

    public Date getDate() {
        return date;
    }

    public Timestamp getTimestamp() {
        return date == null ? null : new Timestamp(date.getTime());
    }

}

