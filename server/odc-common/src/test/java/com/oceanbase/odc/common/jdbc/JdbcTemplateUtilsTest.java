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
package com.oceanbase.odc.common.jdbc;

import java.sql.Statement;

import org.junit.Assert;
import org.junit.Test;

public class JdbcTemplateUtilsTest {

    @Test
    public void batchInsertAffectRows_0_row_affected_expect_0() {
        int rows = JdbcTemplateUtils.batchInsertAffectRows(new int[] {0});
        Assert.assertEquals(0, rows);
    }

    @Test
    public void batchInsertAffectRows_NO_INFO_row_affected_expect_1() {
        int rows = JdbcTemplateUtils.batchInsertAffectRows(new int[] {Statement.SUCCESS_NO_INFO});
        Assert.assertEquals(1, rows);
    }

    @Test
    public void batchInsertAffectRows_null_expect_0() {
        int rows = JdbcTemplateUtils.batchInsertAffectRows(null);
        Assert.assertEquals(0, rows);
    }

    @Test
    public void batchInsertAffectRowsWithBatchSize() {
        int rows =
                JdbcTemplateUtils.batchInsertAffectRowsWithBatchSize(new int[][] {{1, 0}, {Statement.SUCCESS_NO_INFO}});
        Assert.assertEquals(2, rows);
    }
}
