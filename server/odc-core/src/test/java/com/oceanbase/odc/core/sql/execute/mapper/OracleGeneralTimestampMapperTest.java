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
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.core.sql.execute.tool.TimestampCellData;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

/**
 * @author jingtian
 * @date 2023/7/14
 * @since
 */
public class OracleGeneralTimestampMapperTest {
    @Test
    @Ignore("TODO: fix this test")
    public void mapCell_nonNullTimestamp_returnRightValue() throws IOException, SQLException, ParseException {
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp");
        DataType dataType = factory.generate();
        OracleGeneralTimestampMapper mapper = new OracleGeneralTimestampMapper();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String value = "2022-07-14 12:27:11";
        Timestamp timestamp = new Timestamp(format.parse(value).getTime());
        timestamp.setNanos(0);
        Assert.assertEquals("2022-07-14T12:27:11+08:00", mapper.mapCell(new TimestampCellData(timestamp, dataType)));
    }

    @Test
    public void mapCell_nullTimestamp_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("timestamp");
        DataType dataType = factory.generate();
        OracleGeneralTimestampMapper mapper = new OracleGeneralTimestampMapper();
        Assert.assertNull(mapper.mapCell(new TimestampCellData(null, dataType)));
    }
}
