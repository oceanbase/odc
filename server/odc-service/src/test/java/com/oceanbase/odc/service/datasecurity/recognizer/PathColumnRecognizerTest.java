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
package com.oceanbase.odc.service.datasecurity.recognizer;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2023/5/24 15:05
 */
public class PathColumnRecognizerTest {

    @Test
    public void test_recognize_true() {
        ColumnRecognizer recognizer = new PathColumnRecognizer(Arrays.asList("*.*b*.c"), Arrays.asList("a.b.*"));
        Assert.assertTrue(recognizer.recognize(createDBTableColumn("a", "b12", "c")));
        Assert.assertTrue(recognizer.recognize(createDBTableColumn("a12", "34b56", "c")));
        Assert.assertTrue(recognizer.recognize(createDBTableColumn("a12", "34b", "c")));
    }

    @Test
    public void test_recognize_false() {
        ColumnRecognizer recognizer = new PathColumnRecognizer(Arrays.asList("*.*b*.c"), Arrays.asList("a.b.*"));
        Assert.assertFalse(recognizer.recognize(createDBTableColumn("a", "b", "c")));
        Assert.assertFalse(recognizer.recognize(createDBTableColumn("a12", "b34", "c56")));
        Assert.assertFalse(recognizer.recognize(createDBTableColumn("a12", "b34", null)));
    }

    private DBTableColumn createDBTableColumn(String schemaName, String tableName, String columnName) {
        DBTableColumn column = new DBTableColumn();
        column.setSchemaName(schemaName);
        column.setTableName(tableName);
        column.setName(columnName);
        return column;
    }
}
