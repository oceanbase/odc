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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLTableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectTestUtils;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

public class DBTableEditorTest {

    private static DBTableEditor tableEditor;

    @BeforeClass
    public static void setUp() {
        tableEditor = new MySQLTableEditor(new OBMySQLIndexEditor(), new MySQLColumnEditor(),
                new MySQLConstraintEditor(), new MySQLDBTablePartitionEditor());
    }

    @Test
    public void generateUpdateObjectDDLWithoutRenaming_UKAndUniqueIndexNameNotEqual_AddIndexAndUK() {
        DBTable oldTable = getBaseTable();
        DBTable newTable = getBaseTable();
        newTable.setIndexes(Arrays.asList(DBObjectTestUtils.getNewUniqueIndex()));
        newTable.setConstraints(Arrays.asList(DBObjectTestUtils.getNewUK(), DBObjectTestUtils.getNewFK()));
        String ddl = tableEditor.generateUpdateObjectDDLWithoutRenaming(oldTable, newTable);
        Assert.assertEquals("CREATE UNIQUE INDEX `new_unique_index` ON `schema`.`table` (`a`, `b`);\n"
                + "ALTER TABLE `schema`.`table` ADD CONSTRAINT  FOREIGN KEY (`c1`) REFERENCES `schema`.`tb` (`c3`);\n"
                + "ALTER TABLE `schema`.`table` ADD CONSTRAINT `new_unique_key` UNIQUE;\n", ddl);
    }

    @Test
    public void generateUpdateObjectDDLWithoutRenaming_UKAndUniqueIndexNameEqual_OnlyAddUK() {
        DBTable oldTable = getBaseTable();
        DBTable newTable = getBaseTable();

        DBTableIndex uniqueIndex = DBObjectTestUtils.getNewUniqueIndex();
        DBTableConstraint uk = DBObjectTestUtils.getNewUK();
        uk.setName(uniqueIndex.getName());

        newTable.setIndexes(Arrays.asList(uniqueIndex));
        newTable.setConstraints(Arrays.asList(uk, DBObjectTestUtils.getNewFK()));
        String ddl = tableEditor.generateUpdateObjectDDLWithoutRenaming(oldTable, newTable);
        Assert.assertEquals(
                "CREATE UNIQUE INDEX `new_unique_index` ON `schema`.`table` (`a`, `b`);\n"
                        + "ALTER TABLE `schema`.`table` ADD CONSTRAINT  FOREIGN KEY (`c1`) REFERENCES `schema`.`tb` (`c3`);\n",
                ddl);
    }

    private DBTable getBaseTable() {
        DBTable table = new DBTable();
        table.setName("table");
        table.setSchemaName("schema");
        return table;
    }

}
