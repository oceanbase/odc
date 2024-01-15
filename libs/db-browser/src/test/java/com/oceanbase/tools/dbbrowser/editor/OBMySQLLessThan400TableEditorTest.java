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
package com.oceanbase.tools.dbbrowser.editor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400TableEditor;

/**
 * @author jingtian
 * @date 2024/1/12
 * @since ODC_release_4.2.4
 */
public class OBMySQLLessThan400TableEditorTest {
    private DBTableEditor tableEditor;

    @Before
    public void setUp() {
        tableEditor = new OBMySQLLessThan400TableEditor(new OBMySQLIndexEditor(), new MySQLColumnEditor(),
                new MySQLConstraintEditor(), new MySQLDBTablePartitionEditor());
    }

    @Test
    public void generateUpdateObjectDDL() {
        String ddl =
                tableEditor.generateUpdateObjectDDL(DBObjectUtilsTest.getOldTable(), DBObjectUtilsTest.getNewTable());
        Assert.assertEquals(
                "ALTER TABLE `old_table` RENAME TO `whatever_table`;\n"
                        + "/* Unsupported operation to modify table charset */\n"
                        + "/* Unsupported operation to modify table collation */\n",
                ddl);
    }
}
