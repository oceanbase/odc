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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.sql.execute.tool.BitCellData;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

/**
 * Test cases for {@link MySQLNumberMapper}
 *
 * @author yh263208
 * @date 2023-11-10 15:28
 * @since ODC_release_4.22bp
 */
public class MySQLNumberMapperTest {

    @Test
    public void mapCell_nonNullNumber_returnRightValue() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("decimal unsigned");
        DataType dataType = factory.generate();
        MySQLNumberMapper mapper = new MySQLNumberMapper();
        String expect = "0.0000000";
        Assert.assertEquals(expect, mapper.mapCell(new BitCellData(expect.getBytes(), dataType)));
    }

    @Test
    public void mapCell_nullNumber_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("decimal unsigned");
        DataType dataType = factory.generate();
        MySQLNumberMapper mapper = new MySQLNumberMapper();
        Assert.assertNull(mapper.mapCell(new BitCellData(null, dataType)));
    }

}
