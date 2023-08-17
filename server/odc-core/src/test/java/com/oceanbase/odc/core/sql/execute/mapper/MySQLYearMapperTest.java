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
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.sql.execute.tool.DateCellData;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

/**
 * {@link MySQLYearMapper}
 *
 * @author yh263208
 * @date 2022-07-14 12:41
 * @since ODC_release_3.4.0
 */
public class MySQLYearMapperTest {

    @Test
    public void mapCell_nonNullLess1000Year_returnRightValue() throws IOException, SQLException, ParseException {
        DataTypeFactory factory = new CommonDataTypeFactory("year");
        DataType dataType = factory.generate();
        MySQLYearMapper mapper = new MySQLYearMapper();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String value = "123-07-14 12:27:11";
        Assert.assertEquals("0123",
                mapper.mapCell(new DateCellData(new Date(format.parse(value).getTime()), dataType)));
    }

    @Test
    public void mapCell_nonNullMore1000Year_returnRightValue() throws IOException, SQLException, ParseException {
        DataTypeFactory factory = new CommonDataTypeFactory("year");
        DataType dataType = factory.generate();
        MySQLYearMapper mapper = new MySQLYearMapper();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String value = "1223-07-14 12:27:11";
        Assert.assertEquals("1223",
                mapper.mapCell(new DateCellData(new Date(format.parse(value).getTime()), dataType)));
    }

    @Test
    public void mapCell_nullYear_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("year");
        DataType dataType = factory.generate();
        MySQLYearMapper mapper = new MySQLYearMapper();
        Assert.assertNull(mapper.mapCell(new DateCellData(null, dataType)));
    }

    @Test
    public void supports_year_supports() throws IOException, SQLException {
        MySQLYearMapper mapper = new MySQLYearMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("year");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

}


