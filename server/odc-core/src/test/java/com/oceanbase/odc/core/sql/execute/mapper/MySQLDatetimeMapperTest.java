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
package com.oceanbase.odc.core.sql.execute.mapper;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.sql.execute.tool.TimestampCellData;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

/**
 * {@link MySQLDatetimeMapper}
 *
 * @author yh263208
 * @date 2022-07-14 12:38
 * @since ODC_release_3.4.0
 */
public class MySQLDatetimeMapperTest {

    @Test
    public void mapCell_nonNullDatetime_returnRightValue() throws IOException, SQLException, ParseException {
        DataTypeFactory factory = new CommonDataTypeFactory("DATETIME");
        DataType dataType = factory.generate();
        MySQLDatetimeMapper mapper = new MySQLDatetimeMapper();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String value = "2022-07-14 12:27:11";
        Timestamp timestamp = new Timestamp(format.parse(value).getTime());
        timestamp.setNanos(0);
        Assert.assertEquals(value, mapper.mapCell(new TimestampCellData(timestamp, dataType)));
    }

    @Test
    public void mapCell_nullDatetime_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("DATETIME");
        DataType dataType = factory.generate();
        MySQLDatetimeMapper mapper = new MySQLDatetimeMapper();
        Assert.assertNull(mapper.mapCell(new TimestampCellData(null, dataType)));
    }

    @Test
    public void supports_datetime_supports() throws IOException, SQLException {
        MySQLDatetimeMapper mapper = new MySQLDatetimeMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("DATETIME");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_timestamp_notSupports() throws IOException, SQLException {
        MySQLDatetimeMapper mapper = new MySQLDatetimeMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp");
        Assert.assertFalse(mapper.supports(factory.generate()));
    }

}
