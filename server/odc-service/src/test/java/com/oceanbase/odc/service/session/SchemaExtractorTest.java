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

package com.oceanbase.odc.service.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.session.util.SchemaExtractor;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

/**
 * @Author: Lebie
 * @Date: 2023/12/8 15:56
 * @Description: []
 */
public class SchemaExtractorTest {

    @Test
    public void testMySQL_listSchemaName2SqlTypes() {
        String sql = "select * from db1.table1";
        Map<String, Set<SqlType>> actual =
                SchemaExtractor.listSchemaName2SqlTypes(Arrays.asList(SqlTuple.newTuple(sql)), DialectType.OB_MYSQL);
        Assert.assertTrue(actual.keySet().size() == 1 && actual.containsKey("db1"));
        Assert.assertTrue(actual.get("db1").size() == 1 && actual.get("db1").contains(SqlType.SELECT));
    }

    @Test
    public void testOracle_listSchemaName2SqlTypes() {
        String sql = "SELECT * FROM DB1.TABLE1";
        Map<String, Set<SqlType>> actual =
                SchemaExtractor.listSchemaName2SqlTypes(Arrays.asList(SqlTuple.newTuple(sql)), DialectType.OB_ORACLE);
        Assert.assertTrue(actual.keySet().size() == 1 && actual.containsKey("DB1"));
        Assert.assertTrue(actual.get("DB1").size() == 1 && actual.get("DB1").contains(SqlType.SELECT));
    }

    @Test
    public void testOracle_listSchemaName2SqlTypes_WithDBLink() {
        String sql = "SELECT * FROM DB1.TABLE1@FAKE_DBLINK;";
        Map<String, Set<SqlType>> actual =
                SchemaExtractor.listSchemaName2SqlTypes(Arrays.asList(SqlTuple.newTuple(sql)), DialectType.OB_ORACLE);
        Assert.assertTrue(actual.isEmpty());
    }

    @Test
    public void testMySQL_ListSchemaNames() {
        String sql = "select * from db1.table1";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL);
        Set<String> expect = Collections.singleton("db1");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ListSchemaNames_NoDBLink() {
        String sql = "SELECT * FROM DB1.TABLE1";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE);
        Set<String> expect = Collections.singleton("DB1");
        Assert.assertEquals(expect, actual);
    }


    @Test
    public void testOracle_ListSchemaNames_WithDBLink() {
        String sql = "SELECT * FROM DB1.TABLE1@FAKE_DBLINK;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE);
        Assert.assertTrue(actual.isEmpty());
    }

}
