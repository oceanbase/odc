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
 * {@link MySQLBitMapperTest}
 *
 * @author yh263208
 * @date 2022-07-14 12:32
 * @since ODC_release_3.4.0
 */
public class MySQLBitMapperTest {

    @Test
    public void mapCell_nonNullBytes_returnRightValue() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("bit");
        DataType dataType = factory.generate();
        MySQLBitMapper mapper = new MySQLBitMapper();
        byte[] bytes = new byte[] {1, 2, 3};
        Assert.assertEquals("000000010000001000000011", mapper.mapCell(new BitCellData(bytes, dataType)));
    }

    @Test
    public void mapCell_nullBytes_returnNull() throws IOException, SQLException {
        DataTypeFactory factory = new CommonDataTypeFactory("bit");
        DataType dataType = factory.generate();
        MySQLBitMapper mapper = new MySQLBitMapper();
        Assert.assertNull(mapper.mapCell(new BitCellData(null, dataType)));
    }

    @Test
    public void supports_bits_supports() throws IOException, SQLException {
        MySQLBitMapper mapper = new MySQLBitMapper();
        DataTypeFactory factory = new CommonDataTypeFactory("bit");
        Assert.assertTrue(mapper.supports(factory.generate()));
    }

}
