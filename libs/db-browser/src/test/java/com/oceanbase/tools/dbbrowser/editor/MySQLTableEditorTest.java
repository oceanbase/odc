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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBTable;

public class MySQLTableEditorTest {

    private DBTableEditor tableEditor;

    @Before
    public void setUp() {
        tableEditor = new OBMySQLTableEditor(new OBMySQLIndexEditor(), new MySQLColumnEditor(),
                new MySQLConstraintEditor(), new MySQLDBTablePartitionEditor());
    }

    @Test
    public void generateCreateObjectDDL() {
        String ddl = tableEditor.generateCreateObjectDDL(DBObjectUtilsTest.getNewTable());
        Assert.assertEquals("CREATE TABLE `whatever_schema`.`whatever_table` (\n"
                + "`COL1` NUMBER(2, 1) DEFAULT 1 NOT NULL,\n"
                + "`OLD_COL1` NUMBER(2, 1) DEFAULT 1 NOT NULL,\n"
                + "INDEX `new_index` USING HASH (`col1`, `col2`) GLOBAL  COMMENT 'whatever_comment',\n"
                + "INDEX `old_index` USING BTREE (`col1`, `col2`) GLOBAL  COMMENT 'whatever_comment',\n"
                + "CONSTRAINT `old_constraint` FOREIGN KEY (`col1`, `col2`) REFERENCES `ref_schema`.`ref_table` (`ref_col1`, `ref_col2`) "
                + "MATCH FULL ON DELETE SET NULL ON UPDATE NO ACTION,\n"
                + "CONSTRAINT `new_constraint` UNIQUE (`col1`, `col2`)\n"
                + ") AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMPRESSION = 'zstd_1.0' "
                + "REPLICA_NUM = 1 USE_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 COMMENT = 'this is a "
                + "comment'  WITH COLUMN GROUP(each column);\n",
                ddl);
    }

    @Test
    public void generateDropObjectDDL() {
        String ddl = tableEditor.generateDropObjectDDL(DBObjectUtilsTest.getNewTable());
        Assert.assertEquals("DROP TABLE `whatever_schema`.`whatever_table`", ddl);
    }

    @Test
    public void generateUpdateObjectDDL() {
        String ddl =
                tableEditor.generateUpdateObjectDDL(DBObjectUtilsTest.getOldTable(), DBObjectUtilsTest.getNewTable());
        Assert.assertEquals(
                "ALTER TABLE `old_table` RENAME TO `whatever_table`;\n"
                        + "ALTER TABLE `whatever_schema`.`whatever_table` CHARACTER SET = utf8mb4;\n"
                        + "ALTER TABLE `whatever_schema`.`whatever_table` COLLATE = utf8mb4_bin;\n",
                ddl);
    }

    @Test
    public void generateUpdateObjectDDL_addAndDropColumnGroup() {
        DBTable oldTable = DBObjectUtilsTest.getOldTable();
        DBTable newTable = DBObjectUtilsTest.getOldTable();

        DBColumnGroupElement cg = new DBColumnGroupElement();
        cg.setAllColumns(true);
        newTable.setColumnGroups(Collections.singletonList(cg));
        String ddl = tableEditor.generateUpdateObjectDDL(oldTable, newTable);
        Assert.assertEquals(
                "ALTER TABLE `whatever_schema`.`old_table` DROP COLUMN GROUP(each column);\n"
                        + "ALTER TABLE `whatever_schema`.`old_table` ADD COLUMN GROUP(all columns);\n",
                ddl);
    }

    @Test
    public void generateRenameObjectDDL() {
        String ddl =
                tableEditor.generateRenameObjectDDL(DBObjectUtilsTest.getOldTable(), DBObjectUtilsTest.getNewTable());
        Assert.assertEquals("ALTER TABLE `old_table` RENAME TO `whatever_table`", ddl);
    }

}
