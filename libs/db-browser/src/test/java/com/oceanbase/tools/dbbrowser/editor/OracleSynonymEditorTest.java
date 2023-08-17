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
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.editor.oracle.OracleSynonymEditor;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;

/**
 * {@link OracleSynonymEditorTest}
 *
 * @author yh263208
 * @date 2023-02-23 16:04
 * @since db-browser-1.0.0_SNAPSHOT
 */
public class OracleSynonymEditorTest {

    @Test
    public void generateCreateObjectDDL_publicSynonym_generateSucceed() {
        DBObjectEditor<DBSynonym> editor = new OracleSynonymEditor();
        DBSynonym synonym = new DBSynonym();
        synonym.setSynonymType(DBSynonymType.PUBLIC);
        synonym.setSynonymName("test_synonym");
        synonym.setTableOwner("root");
        synonym.setTableName("test_emp");

        String expect = "CREATE OR REPLACE PUBLIC SYNONYM \"test_synonym\" FOR \"root\".\"test_emp\";";
        Assert.assertEquals(expect, editor.generateCreateDefinitionDDL(synonym));
    }

    @Test
    public void generateCreateObjectDDL_commonSynonym_generateSucceed() {
        DBObjectEditor<DBSynonym> editor = new OracleSynonymEditor();
        DBSynonym synonym = new DBSynonym();
        synonym.setSynonymType(DBSynonymType.COMMON);
        synonym.setSynonymName("test_synonym");
        synonym.setTableOwner("root");
        synonym.setTableName("test_emp");

        String expect = "CREATE OR REPLACE SYNONYM \"test_synonym\" FOR \"root\".\"test_emp\";";
        Assert.assertEquals(expect, editor.generateCreateDefinitionDDL(synonym));
    }

}
