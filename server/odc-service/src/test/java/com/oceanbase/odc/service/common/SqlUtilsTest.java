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
package com.oceanbase.odc.service.common;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.common.util.SqlUtils;

public class SqlUtilsTest {

    @Test
    public void isCurrentTimestampExpression_CurrentTimestamp_ReturnTrue() {
        boolean matched = SqlUtils.isCurrentTimestampExpression("CURRENT_TIMESTAMP");
        Assert.assertTrue(matched);
    }

    @Test
    public void isCurrentTimestampExpression_Blank_ReturnFalse() {
        boolean matched = SqlUtils.isCurrentTimestampExpression(null);
        Assert.assertFalse(matched);
    }

    @Test
    public void isCurrentTimestampExpression_NotCurrentTimestamp_ReturnFalse() {
        boolean matched = SqlUtils.isCurrentTimestampExpression("self_defined_function()");
        Assert.assertFalse(matched);
    }

    @Test
    public void removeComments() {
        String sql = "select xx from table -- this is comment";
        SqlCommentProcessor commentProcessor = new SqlCommentProcessor(DialectType.OB_ORACLE, false, false);
        String resultSql = SqlUtils.removeComments(commentProcessor, sql);
        Assert.assertEquals("select xx from table ", resultSql);
    }

    @Test
    public void replaceTemporaryTableName_WithOracleUserPrefix() {
        String sql = "CREATE GLOBAL TEMPORARY TABLE \"TABLE1\"(T1 TABLE1_C1)";
        String s = StringUtils.replace(sql, " TABLE ",
                " TABLE " + StringUtils.quoteOracleIdentifier("SYS") + ".", 1);
        Assert.assertEquals("CREATE GLOBAL TEMPORARY TABLE \"SYS\".\"TABLE1\"(T1 TABLE1_C1)", s);

        sql = "CREATE SHARDED TABLE \"TABLE1\"(T1 TABLE1_C1)";
        s = StringUtils.replace(sql, " TABLE ",
                " TABLE " + StringUtils.quoteOracleIdentifier("SYS") + ".", 1);
        Assert.assertEquals("CREATE SHARDED TABLE \"SYS\".\"TABLE1\"(T1 TABLE1_C1)", s);

    }

}
