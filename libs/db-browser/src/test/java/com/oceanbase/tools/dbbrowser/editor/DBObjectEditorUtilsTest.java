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

import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectTestUtils;
import com.oceanbase.tools.dbbrowser.util.DBObjectEditorUtils;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

public class DBObjectEditorUtilsTest {

    private static DBTableConstraintEditor constraintEditor;

    @BeforeClass
    public static void setUp() {
        constraintEditor = new MySQLConstraintEditor();
    }

    @Test
    public void generateShadowTableConstraintListUpdateDDL_AllKeyChanged_Success() {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        DBObjectEditorUtils.generateShadowTableConstraintListUpdateDDL(
                Arrays.asList(DBObjectTestUtils.getOldUK(), DBObjectTestUtils.getOldFK()),
                Arrays.asList(DBObjectTestUtils.getNewUK(),
                        DBObjectTestUtils.getNewFK()),
                constraintEditor, sqlBuilder);
        Assert.assertEquals(
                "ALTER TABLE `schema`.`table` ADD CONSTRAINT  FOREIGN KEY (`c1`) REFERENCES `schema`.`tb` (`c3`);\n"
                        + "ALTER TABLE `schema`.`table` DROP FOREIGN KEY `old_fk`;\n"
                        + "ALTER TABLE `schema`.`table` ADD CONSTRAINT `new_unique_key` UNIQUE;\n"
                        + "ALTER TABLE `schema`.`table` DROP KEY `old_unique_key`;\n",
                sqlBuilder.toString());
    }

    @Test
    public void generateShadowTableConstraintListUpdateDDL_FKChanged_Success() {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        DBObjectEditorUtils.generateShadowTableConstraintListUpdateDDL(
                Arrays.asList(DBObjectTestUtils.getOldUK(), DBObjectTestUtils.getOldFK()),
                Arrays.asList(DBObjectTestUtils.getOldUK(),
                        DBObjectTestUtils.getNewFK()),
                constraintEditor, sqlBuilder);
        Assert.assertEquals(
                "ALTER TABLE `schema`.`table` ADD CONSTRAINT  FOREIGN KEY (`c1`) REFERENCES `schema`.`tb` (`c3`);\n"
                        + "ALTER TABLE `schema`.`table` DROP FOREIGN KEY `old_fk`;\n",
                sqlBuilder.toString());
    }

    @Test
    public void generateShadowTableConstraintListUpdateDDL_UKChanged_Success() {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        DBObjectEditorUtils.generateShadowTableConstraintListUpdateDDL(
                Arrays.asList(DBObjectTestUtils.getOldUK(), DBObjectTestUtils.getOldFK()),
                Arrays.asList(DBObjectTestUtils.getNewUK(),
                        DBObjectTestUtils.getOldFK()),
                constraintEditor, sqlBuilder);
        Assert.assertEquals("ALTER TABLE `schema`.`table` ADD CONSTRAINT `new_unique_key` UNIQUE;\n"
                + "ALTER TABLE `schema`.`table` DROP KEY `old_unique_key`;\n", sqlBuilder.toString());
    }

    @Test
    public void generateShadowTableConstraintListUpdateDDL_NoKeyChanged_Success() {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        DBObjectEditorUtils.generateShadowTableConstraintListUpdateDDL(
                Arrays.asList(DBObjectTestUtils.getOldUK(), DBObjectTestUtils.getOldFK()),
                Arrays.asList(DBObjectTestUtils.getOldUK(),
                        DBObjectTestUtils.getOldFK()),
                constraintEditor, sqlBuilder);
        Assert.assertEquals("", sqlBuilder.toString());
    }

}
