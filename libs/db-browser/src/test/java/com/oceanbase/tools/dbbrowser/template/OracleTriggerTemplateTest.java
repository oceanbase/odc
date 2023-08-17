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
package com.oceanbase.tools.dbbrowser.template;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.model.DBTriggerDMLEvent;
import com.oceanbase.tools.dbbrowser.model.DBTriggerEvent;
import com.oceanbase.tools.dbbrowser.model.DBTriggerMode;
import com.oceanbase.tools.dbbrowser.model.DBTriggerReference;
import com.oceanbase.tools.dbbrowser.model.DBTriggerReferenceType;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleTriggerTemplate;

/**
 * {@link OracleTriggerTemplateTest}
 *
 * @author yh263208
 * @date 2023-02-23 16:54
 * @since db-browser_1.0.0_SNAPSHOT
 */
public class OracleTriggerTemplateTest {

    @Test
    public void generateCreateObjectTemplate_enabledTrigger_generateSucceed() {
        DBObjectTemplate<DBTrigger> editor = new OracleTriggerTemplate();
        DBTrigger trigger = generateTrigger();
        String expect = "CREATE OR REPLACE TRIGGER \"TEST_TRIGGER\" BEFORE\n"
                + "\tINSERT OR UPDATE OF \"col\" ON \"emp\"\n"
                + "\tREFERENCING NEW AS hello OLD AS hello_old\n"
                + "\tFOR EACH ROW\n"
                + "\tENABLE\n"
                + "\tWHEN (select 3+4 from dual)\n"
                + "BEGIN\n"
                + "\t --your trigger body\n"
                + "END;";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(trigger));
    }

    @Test
    public void generateCreateObjectTemplate_disabledTrigger_generateSucceed() {
        DBObjectTemplate<DBTrigger> editor = new OracleTriggerTemplate();
        DBTrigger trigger = generateTrigger();
        trigger.setEnable(false);
        String expect = "CREATE OR REPLACE TRIGGER \"TEST_TRIGGER\" BEFORE\n"
                + "\tINSERT OR UPDATE OF \"col\" ON \"emp\"\n"
                + "\tREFERENCING NEW AS hello OLD AS hello_old\n"
                + "\tFOR EACH ROW\n"
                + "\tDISABLE\n"
                + "\tWHEN (select 3+4 from dual)\n"
                + "BEGIN\n"
                + "\t --your trigger body\n"
                + "END;";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(trigger));
    }

    @Test
    public void generateCreateObjectTemplate_nonRowLevelTrigger_generateSucceed() {
        DBObjectTemplate<DBTrigger> editor = new OracleTriggerTemplate();
        DBTrigger trigger = generateTrigger();
        trigger.setRowLevel(false);
        String expect = "CREATE OR REPLACE TRIGGER \"TEST_TRIGGER\" BEFORE\n"
                + "\tINSERT OR UPDATE OF \"col\" ON \"emp\"\n"
                + "\tREFERENCING NEW AS hello OLD AS hello_old\n"
                + "\tENABLE\n"
                + "\tWHEN (select 3+4 from dual)\n"
                + "BEGIN\n"
                + "\t --your trigger body\n"
                + "END;";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(trigger));
    }

    private DBTrigger generateTrigger() {
        DBTrigger trigger = new DBTrigger();
        trigger.setTriggerName("TEST_TRIGGER");
        trigger.setEnable(true);
        trigger.setRowLevel(true);
        trigger.setSchemaMode("root");
        trigger.setSchemaName("emp");
        trigger.setSqlExpression("select 3+4 from dual");
        trigger.setTriggerMode(DBTriggerMode.BEFORE);
        List<DBTriggerReference> references = new ArrayList<>();
        DBTriggerReference reference = new DBTriggerReference();
        reference.setReferenceType(DBTriggerReferenceType.NEW);
        reference.setReferName("hello");
        references.add(reference);
        reference = new DBTriggerReference();
        reference.setReferenceType(DBTriggerReferenceType.OLD);
        reference.setReferName("hello_old");
        references.add(reference);
        trigger.setReferences(references);
        List<DBTriggerEvent> events = new ArrayList<>();
        DBTriggerEvent event = new DBTriggerEvent();
        event.setDmlEvent(DBTriggerDMLEvent.INSERT);
        events.add(event);
        event = new DBTriggerEvent();
        event.setDmlEvent(DBTriggerDMLEvent.UPDATE);
        event.setColumn("col");
        events.add(event);
        trigger.setTriggerEvents(events);
        return trigger;
    }

}
