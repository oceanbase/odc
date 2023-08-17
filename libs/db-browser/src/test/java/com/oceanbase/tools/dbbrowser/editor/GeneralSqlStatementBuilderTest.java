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

import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;

/**
 * {@link GeneralSqlStatementBuilderTest}
 *
 * @author yh263208
 * @date 2023-03-07 12:01
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class GeneralSqlStatementBuilderTest {

    @Test
    public void drop_dropPublicSynonym_generateSucceed() {
        String actual = GeneralSqlStatementBuilder.drop(new OracleSqlBuilder(),
                DBObjectType.PUBLIC_SYNONYM, null, "test");
        String expect = "DROP PUBLIC SYNONYM \"test\"";
        Assert.assertEquals(actual, expect);
    }

    @Test
    public void drop_dropPackageBody_generateSucceed() {
        String actual = GeneralSqlStatementBuilder.drop(new OracleSqlBuilder(),
                DBObjectType.PACKAGE_BODY, null, "test");
        String expect = "DROP PACKAGE BODY \"test\"";
        Assert.assertEquals(actual, expect);
    }

}
