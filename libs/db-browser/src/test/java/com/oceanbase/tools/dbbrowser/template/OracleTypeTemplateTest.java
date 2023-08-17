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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.model.DBTypeCode;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleTypeTemplate;

/**
 * {@link OracleTypeTemplateTest}
 *
 * @author yh263208
 * @date 2023-02-23 17:24
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OracleTypeTemplateTest {

    @Test
    public void generateCreateObjectTemplate_objectType_generateSucceed() {
        DBObjectTemplate<DBType> template = new OracleTypeTemplate();
        DBType type = new DBType();
        type.setTypeName("test_type");
        type.setTypeCode(DBTypeCode.OBJECT);

        String expect = "CREATE OR REPLACE TYPE \"test_type\"\n"
                + "AS OBJECT(/* TODO enter attribute and method declarations here */)";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(type));
    }

    @Test
    public void generateCreateObjectTemplate_varrayType_generateSucceed() {
        DBObjectTemplate<DBType> template = new OracleTypeTemplate();
        DBType type = new DBType();
        type.setTypeName("test_type");
        type.setTypeCode(DBTypeCode.VARRAY);

        String expect = "CREATE OR REPLACE TYPE \"test_type\"\n"
                + "AS VARRAY(/* array size */) OF /* datatype */";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(type));
    }

    @Test
    public void generateCreateObjectTemplate_tableType_generateSucceed() {
        DBObjectTemplate<DBType> template = new OracleTypeTemplate();
        DBType type = new DBType();
        type.setTypeName("test_type");
        type.setTypeCode(DBTypeCode.TABLE);

        String expect = "CREATE OR REPLACE TYPE \"test_type\"\n"
                + "AS TABLE OF /* datatype */";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(type));
    }

}
