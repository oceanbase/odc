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

import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleMViewEditor;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;

/**
 * @description: all tests for {@link OBOracleMViewEditor}
 * @author: zijia.cj
 * @date: 2025/4/2 21:55
 * @since: 4.3.4
 */
public class OBOracleMViewEditorTest {

    private OBOracleMViewEditor mViewEditor;

    @Before
    public void setUp() {
        mViewEditor = new OBOracleMViewEditor(new OBOracleIndexEditor());
    }

    @Test
    public void generateDropObjectDDL() {
        String ddl = mViewEditor.generateDropObjectDDL(DBObjectUtilsTest.getNewMView());
        Assert.assertEquals("DROP MATERIALIZED VIEW \"whatever_schema\".\"whatever_table\"", ddl);
    }

    @Test
    public void generateUpdateObjectDDL_dropIndex_generateSucceed() {
        DBMaterializedView newMView = DBObjectUtilsTest.getNewMView();
        newMView.setIndexes(null);
        String ddl = mViewEditor.generateUpdateObjectDDL(DBObjectUtilsTest.getOldMView(), newMView);
        Assert.assertEquals("DROP INDEX \"old_index\";\n", ddl);
    }

    @Test
    public void generateUpdateObjectDDL_createIndex_generateSucceed() {
        DBMaterializedView oldMView = DBObjectUtilsTest.getOldMView();
        oldMView.setIndexes(null);
        String ddl = mViewEditor.generateUpdateObjectDDL(oldMView, DBObjectUtilsTest.getNewMView());
        Assert.assertEquals(
                "CREATE  INDEX \"new_index\" USING HASH ON \"whatever_schema\".\"whatever_table\" (\"col1\", \"col2\") GLOBAL;\n",
                ddl);
    }

    @Test
    public void generateUpdateObjectDDL_updateNameIndex_generateSucceed() {
        DBMaterializedView updatedName = DBObjectUtilsTest.getOldMView();
        updatedName.getIndexes().get(0).setName("new_index");
        String ddl = mViewEditor.generateUpdateObjectDDL(DBObjectUtilsTest.getOldMView(), updatedName);
        Assert.assertEquals("ALTER INDEX \"old_index\" RENAME TO \"new_index\";\n", ddl);
    }

    @Test
    public void generateUpdateObjectDDL_updateVisibleIndex_generateSucceed() {
        DBMaterializedView updatedVisible = DBObjectUtilsTest.getOldMView();
        updatedVisible.getIndexes().get(0).setVisible(false);
        String ddl = mViewEditor.generateUpdateObjectDDL(DBObjectUtilsTest.getOldMView(), updatedVisible);
        Assert.assertEquals("ALTER TABLE \"whatever_schema\".\"whatever_table\" ALTER INDEX \"old_index\" INVISIBLE;\n",
                ddl);
    }

    @Test
    public void generateUpdateObjectDDL_dropAndCreateIndex_generateSucceed() {
        DBMaterializedView newMView = DBObjectUtilsTest.getNewMView();
        newMView.getIndexes().get(0).setOrdinalPosition(1);
        String ddl = mViewEditor.generateUpdateObjectDDL(DBObjectUtilsTest.getOldMView(), newMView);
        Assert.assertEquals("DROP INDEX \"old_index\";\n" +
                "CREATE  INDEX \"new_index\" USING HASH ON \"whatever_schema\".\"whatever_table\" (\"col1\", \"col2\") GLOBAL;\n",
                ddl);
    }

}
