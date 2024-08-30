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
