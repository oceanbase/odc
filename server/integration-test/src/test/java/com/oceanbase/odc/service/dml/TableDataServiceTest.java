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
package com.oceanbase.odc.service.dml;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.dml.model.BatchDataModifyReq;
import com.oceanbase.odc.service.dml.model.BatchDataModifyReq.Operate;
import com.oceanbase.odc.service.dml.model.BatchDataModifyReq.Row;
import com.oceanbase.odc.service.dml.model.BatchDataModifyResp;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;

public class TableDataServiceTest extends ServiceTestEnv {

    @Autowired
    private TableDataService tableDataService;

    @Test(expected = BadArgumentException.class)
    public void batchGetModifySql_AllOperates() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        BatchDataModifyReq req = new BatchDataModifyReq();
        req.setSchemaName("test");
        req.setTableName("t_test");
        req.setRows(Arrays.asList(row(Operate.INSERT), row(Operate.UPDATE), row(Operate.DELETE)));
        tableDataService.batchGetModifySql(connectionSession, req);
    }

    @Test
    public void batchGetModifySql_PrimaryKey() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        SyncJdbcExecutor jdbcExecutor =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcExecutor.execute("create table \"t_test_1\"(\"id\" integer primary key,\"c1\" varchar(64))");
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        try {
            BatchDataModifyReq req = new BatchDataModifyReq();
            req.setSchemaName(schema);
            req.setTableName("t_test_1");
            req.setRows(Arrays.asList(row_with_primay_key(Operate.INSERT), row_with_primay_key(Operate.UPDATE),
                    row_with_primay_key(Operate.DELETE)));
            BatchDataModifyResp resp = tableDataService.batchGetModifySql(connectionSession, req);

            BatchDataModifyResp expected = new BatchDataModifyResp();
            expected.setSchemaName(schema);
            expected.setTableName("t_test_1");
            expected.setCreateRows(1);
            expected.setUpdateRows(1);
            expected.setDeleteRows(1);
            expected.setSql("insert into \"" + schema + "\".\"t_test_1\"(\"c1\",\"id\") values('b2',2);\n"
                    + "update \"" + schema + "\".\"t_test_1\" set \"c1\" = 'b2', \"id\" = 2 where \"id\"=1;\n"
                    + "delete from \"" + schema + "\".\"t_test_1\" where \"id\"=1;\n");
            Assert.assertEquals(expected, resp);
        } finally {
            jdbcExecutor.execute("drop table \"t_test_1\"");
        }
    }

    @Test
    public void batchGetModifySql_RowId() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        BatchDataModifyReq req = new BatchDataModifyReq();
        req.setSchemaName("test");
        req.setTableName("t_test");
        req.setRows(Arrays.asList(row_with_row_id(Operate.INSERT), row_with_row_id(Operate.UPDATE),
                row_with_row_id(Operate.DELETE)));
        BatchDataModifyResp resp = tableDataService.batchGetModifySql(connectionSession, req);

        BatchDataModifyResp expected = new BatchDataModifyResp();
        expected.setSchemaName("test");
        expected.setTableName("t_test");
        expected.setCreateRows(1);
        expected.setUpdateRows(1);
        expected.setDeleteRows(1);
        expected.setSql("insert into \"test\".\"t_test\"(\"c1\",\"rowid\") values('b2',2);\n"
                + "update \"test\".\"t_test\" set \"c1\" = 'b2', \"rowid\" = 2 where \"rowid\"=1;\n"
                + "delete from \"test\".\"t_test\" where \"rowid\"=1;\n");
        Assert.assertEquals(expected, resp);
    }

    @Test
    public void batchGetModifySql_UniqueConstraint() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcOperations.execute("CREATE TABLE \"t_test\" (\n"
                + "\"a\" INTEGER NULL,\n"
                + "\"b\" INTEGER NULL,\n"
                + "\"c\" INTEGER NULL,\n"
                + "CONSTRAINT \"abcd\" UNIQUE (\"a\",\"b\",\"c\")\n"
                + ") ;");
        BatchDataModifyReq req = new BatchDataModifyReq();
        req.setSchemaName(null);
        req.setTableName("t_test");
        req.setRows(Arrays.asList(row_with_unique_constraint(Operate.INSERT),
                row_with_unique_constraint(Operate.UPDATE), row_with_unique_constraint(Operate.DELETE)));
        BatchDataModifyResp resp = tableDataService.batchGetModifySql(connectionSession, req);

        BatchDataModifyResp expected = new BatchDataModifyResp();
        expected.setSchemaName(null);
        expected.setTableName("t_test");
        expected.setCreateRows(1);
        expected.setUpdateRows(1);
        expected.setDeleteRows(1);
        expected.setSql("insert into \"t_test\"(\"a\",\"b\",\"c\") values('a2','b2','c2');\n"
                + "update \"t_test\" set \"a\" = 'a2', \"b\" = 'b2', \"c\" = 'c2' where \"a\"='a1' and \"b\"='b1' and \"c\"='c1';\n"
                + "delete from \"t_test\" where \"a\"='a1' and \"b\"='b1' and \"c\"='c1';\n");
        Assert.assertEquals(expected, resp);
    }

    private Row row(Operate operate) {
        Row row = new Row();
        row.setOperate(operate);
        List<DataModifyUnit> units = new LinkedList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnType("varchar(64)");
        unit.setColumnName("c1");
        unit.setOldData("a1");
        unit.setNewData("b2");
        units.add(unit);
        row.setUnits(units);
        return row;
    }

    private Row row_with_primay_key(Operate operate) {
        Row row = new Row();
        row.setOperate(operate);
        List<DataModifyUnit> units = new LinkedList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnType("varchar(64)");
        unit.setColumnName("c1");
        unit.setOldData("a1");
        unit.setNewData("b2");
        units.add(unit);

        DataModifyUnit primaryUnit = new DataModifyUnit();
        primaryUnit.setColumnType("Integer");
        primaryUnit.setColumnName("id");
        primaryUnit.setOldData("1");
        primaryUnit.setNewData("2");
        units.add(primaryUnit);

        row.setUnits(units);
        return row;
    }

    private Row row_with_row_id(Operate operate) {
        Row row = new Row();
        row.setOperate(operate);
        List<DataModifyUnit> units = new LinkedList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnType("varchar(64)");
        unit.setColumnName("c1");
        unit.setOldData("a1");
        unit.setNewData("b2");
        units.add(unit);

        DataModifyUnit rowIdUnit = new DataModifyUnit();
        rowIdUnit.setColumnType("Integer");
        rowIdUnit.setColumnName("rowid");
        rowIdUnit.setOldData("1");
        rowIdUnit.setNewData("2");
        units.add(rowIdUnit);

        row.setUnits(units);
        return row;
    }

    private Row row_with_unique_constraint(Operate operate) {
        Row row = new Row();
        row.setOperate(operate);
        List<DataModifyUnit> units = new LinkedList<>();
        DataModifyUnit unit1 = new DataModifyUnit();
        unit1.setColumnType("varchar(64)");
        unit1.setColumnName("a");
        unit1.setOldData("a1");
        unit1.setNewData("a2");
        units.add(unit1);

        DataModifyUnit unit2 = new DataModifyUnit();
        unit2.setColumnType("varchar(64)");
        unit2.setColumnName("b");
        unit2.setOldData("b1");
        unit2.setNewData("b2");
        units.add(unit2);

        DataModifyUnit unit3 = new DataModifyUnit();
        unit3.setColumnType("varchar(64)");
        unit3.setColumnName("c");
        unit3.setOldData("c1");
        unit3.setNewData("c2");
        units.add(unit3);

        row.setUnits(units);
        return row;
    }

}
