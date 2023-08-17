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
 * {@link MySQLTimestampMapper} test cases
 *
 * @author yh263208
 * @date 2022-07-14 12:22
 * @since ODC_release_3.4.0
 */
public class MySQLTimestampMapperTest {

    @Test
    public void mapCell_nonNullTimestamp_returnRightValue() throws IOException, SQLException, ParseException {
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp");
        DataType dataType = factory.generate();
        MySQLTimestampMapper mapper = new MySQLTimestampMapper();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String value = "2022-07-14 12:27:11";
        Timestamp timestamp = new Timestamp(format.parse(value).getTime());
        timestamp.setNanos(0);
        Assert.assertEquals(value, mapper.mapCell(new TimestampCellData(timestamp, dataType)));
    }

    @Test
    public void mapCell_nullTimestamp_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp");
        DataType dataType = factory.generate();
        MySQLTimestampMapper mapper = new MySQLTimestampMapper();
        Assert.assertNull(mapper.mapCell(new TimestampCellData(null, dataType)));
    }

    @Test
    public void supports_timestamp_supports() throws IOException, SQLException {
        MySQLTimestampMapper mapper = new MySQLTimestampMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp(3)");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

    @Test
    public void supports_timestampLTZ_notSupports() throws IOException, SQLException {
        MySQLTimestampMapper mapper = new MySQLTimestampMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp(2) with local time zone");
        Assert.assertFalse(mapper.supports(factory.generate()));
    }

}
