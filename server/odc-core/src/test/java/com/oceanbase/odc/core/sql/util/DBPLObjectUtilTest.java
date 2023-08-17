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
package com.oceanbase.odc.core.sql.util;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.tools.dbbrowser.model.DBBasicPLObject;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;

/**
 * {@link DBPLObjectUtilTest}
 *
 * @author yh263208
 * @date 2023-03-06 14:01
 * @since ODC_release_4.2.0
 */
public class DBPLObjectUtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void checkParams_intDataTypeStringInput_expThrown() {
        DBPLParam param = new DBPLParam();
        param.setDataType("int");
        param.setDefaultValue("David");
        param.setParamMode(DBPLParamMode.INOUT);

        DBBasicPLObject object = new DBBasicPLObject();
        object.setParams(Collections.singletonList(param));
        thrown.expectMessage("Param value={David} and param type={int} is not matched");
        thrown.expect(BadArgumentException.class);
        DBPLObjectUtil.checkParams(object);
    }

    @Test
    public void getMySQLParamString_inoutParam_returnInout() {
        Assert.assertEquals("inout", DBPLObjectUtil.getMySQLParamString(DBPLParamMode.INOUT));
    }

    @Test
    public void getMySQLParamString_outParam_returnOut() {
        Assert.assertEquals("out", DBPLObjectUtil.getMySQLParamString(DBPLParamMode.OUT));
    }

    @Test
    public void getMySQLParamString_inParam_returnIn() {
        Assert.assertEquals("in", DBPLObjectUtil.getMySQLParamString(DBPLParamMode.IN));
    }

    @Test
    public void getMySQLParamString_unknownParam_expThrown() {
        thrown.expectMessage("Unsupported param type 'UNKNOWN'");
        thrown.expect(UnsupportedException.class);
        DBPLObjectUtil.getMySQLParamString(DBPLParamMode.UNKNOWN);
    }

    @Test
    public void getOracleParamString_inoutParam_returnInout() {
        Assert.assertEquals("in out", DBPLObjectUtil.getOracleParamString(DBPLParamMode.INOUT));
    }

    @Test
    public void getOracleParamString_outParam_returnOut() {
        Assert.assertEquals("out", DBPLObjectUtil.getOracleParamString(DBPLParamMode.OUT));
    }

    @Test
    public void getOracleParamString_inParam_returnIn() {
        Assert.assertEquals("in", DBPLObjectUtil.getOracleParamString(DBPLParamMode.IN));
    }

    @Test
    public void getOracleParamString_unknownParam_expThrown() {
        thrown.expectMessage("Unsupported param type 'UNKNOWN'");
        thrown.expect(UnsupportedException.class);
        DBPLObjectUtil.getOracleParamString(DBPLParamMode.UNKNOWN);
    }

}
