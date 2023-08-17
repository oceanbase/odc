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

import com.oceanbase.tools.dbbrowser.editor.oracle.OracleSequenceEditor;
import com.oceanbase.tools.dbbrowser.model.DBSequence;

/**
 * {@link OracleSequenceEditorTest}
 *
 * @author yh263208
 * @date 2023-02-23 15:43
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OracleSequenceEditorTest {

    @Test
    public void generateCreateObjectDDL_sequence_generateSucceed() {
        DBObjectEditor<DBSequence> editor = new OracleSequenceEditor();
        DBSequence sequence = new DBSequence();
        sequence.setName("seq_t");
        sequence.setMaxValue("1000");
        sequence.setStartValue("10");
        sequence.setIncreament(2L);
        sequence.setCached(false);
        sequence.setOrderd(true);
        sequence.setCycled(true);

        String expect =
                "CREATE SEQUENCE \"seq_t\" NOMINVALUE MAXVALUE 1000 START WITH 10 INCREMENT BY 2 NOCACHE ORDER CYCLE;";
        Assert.assertEquals(expect, editor.generateCreateDefinitionDDL(sequence));
    }

    @Test
    public void generateUpdateObjectDDL_sequence_generateSucceed() {
        DBObjectEditor<DBSequence> editor = new OracleSequenceEditor();
        DBSequence newObject = new DBSequence();
        newObject.setName("seq_t");
        newObject.setMaxValue("1001");
        newObject.setIncreament(3L);
        newObject.setOrderd(false);
        newObject.setCached(false);
        newObject.setCycled(false);

        DBSequence oldObject = new DBSequence();
        oldObject.setName(newObject.getName());

        String expect = "ALTER SEQUENCE \"seq_t\" NOMINVALUE MAXVALUE 1001 INCREMENT BY 3 NOCACHE NOORDER NOCYCLE;";
        Assert.assertEquals(expect, editor.generateUpdateObjectDDL(oldObject, newObject));
    }

}
