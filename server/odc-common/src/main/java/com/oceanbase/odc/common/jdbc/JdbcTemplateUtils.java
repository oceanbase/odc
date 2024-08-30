package com.oceanbase.odc.common.jdbc;

import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JdbcTemplateUtils {

    public static int batchInsertAffectRowsWithBatchSize(int[][] results) {
        if (results == null) {
            return 0;
        }
        int affectRows = 0;
        for (int[] result : results) {
            affectRows += batchInsertAffectRows(result);
        }
        return affectRows;
    }

    public static int batchInsertAffectRows(int[] result) {
        if (result == null) {
            return 0;
        }
        int affectRows = 0;
        for (int rowsAffected : result) {
            if (rowsAffected > 0 || rowsAffected == Statement.SUCCESS_NO_INFO) {
                affectRows++;
            }
        }
        return affectRows;
    }

}
