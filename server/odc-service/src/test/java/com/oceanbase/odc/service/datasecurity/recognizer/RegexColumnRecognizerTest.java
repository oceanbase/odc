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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2023/5/24 16:21
 */
public class RegexColumnRecognizerTest {

    @Test
    public void recognize_returnTrue() {
        ColumnRecognizer recognizer = createRegexColumnRecognizer();
        Assert.assertTrue(recognizer.recognize(createDBTableColumn("xxx", "xxx", "user_email", "email of user")));
    }

    @Test
    public void recognize_returnFalse() {
        ColumnRecognizer recognizer = createRegexColumnRecognizer();
        Assert.assertFalse(recognizer.recognize(createDBTableColumn("xxx", "xxx", "user_email", null)));
        Assert.assertFalse(recognizer.recognize(createDBTableColumn("   ", "xxx", "user_email", null)));
        Assert.assertFalse(recognizer.recognize(createDBTableColumn("xxx", "xxx", "user", "email")));
    }

    private RegexColumnRecognizer createRegexColumnRecognizer() {
        return new RegexColumnRecognizer("^\\S+$", "^\\S+$", "^\\S*email\\S*$", "^[\\S\\s]*email[\\S\\s]*$");
    }

    private DBTableColumn createDBTableColumn(String schemaName, String tableName, String columnName, String comment) {
        DBTableColumn column = new DBTableColumn();
        column.setSchemaName(schemaName);
        column.setTableName(tableName);
        column.setName(columnName);
        column.setComment(comment);
        return column;
    }
}
