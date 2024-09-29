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
package com.oceanbase.odc.service.connection.logicaldatabase;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;

public class LogicalDatabaseUtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getDataNodesFromCreateTable_PhysicalDatabaseExists() {
        String sql = "create table `db_[0-3].tb_[0-3]` (a varchar(32))";
        Map<String, DataNode> databaseName2DataNodes = new HashMap<>();
        databaseName2DataNodes.put("db_0", new DataNode(1L, "db_0"));
        databaseName2DataNodes.put("db_1", new DataNode(2L, "db_1"));
        databaseName2DataNodes.put("db_2", new DataNode(3L, "db_2"));
        databaseName2DataNodes.put("db_3", new DataNode(4L, "db_3"));

        Set<DataNode> actual =
                LogicalDatabaseUtils.getDataNodesFromCreateTable(sql, DialectType.MYSQL, databaseName2DataNodes);
        Set<DataNode> expected = new HashSet<>(Arrays.asList(
                new DataNode("db_0", "tb_0", 1L),
                new DataNode("db_1", "tb_1", 2L),
                new DataNode("db_2", "tb_2", 3L),
                new DataNode("db_3", "tb_3", 4L)));
        assertEquals(expected, actual);
    }

    @Test
    public void getDataNodesFromCreateTable_PhysicalDatabaseNotExists() {
        String sql = "create table `db_[0-3].tb_[0-3]` (a varchar(32))";
        Map<String, DataNode> databaseName2DataNodes = new HashMap<>();
        databaseName2DataNodes.put("db_0", new DataNode(1L, "db_0"));
        databaseName2DataNodes.put("db_1", new DataNode(2L, "db_1"));
        databaseName2DataNodes.put("db_2", new DataNode(3L, "db_2"));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("physical database not found, database name=db_3");
        LogicalDatabaseUtils.getDataNodesFromCreateTable(sql, DialectType.MYSQL, databaseName2DataNodes);
    }

    @Test
    public void getDataNodesFromCreateTable_WithSchema_IgnoreTheSchema() {
        String sql = "create table `any_schema`.`db_[0-3].tb_[0-3]` (a varchar(32))";
        Map<String, DataNode> databaseName2DataNodes = new HashMap<>();
        databaseName2DataNodes.put("db_0", new DataNode(1L, "db_0"));
        databaseName2DataNodes.put("db_1", new DataNode(2L, "db_1"));
        databaseName2DataNodes.put("db_2", new DataNode(3L, "db_2"));
        databaseName2DataNodes.put("db_3", new DataNode(4L, "db_3"));

        Set<DataNode> actual =
                LogicalDatabaseUtils.getDataNodesFromCreateTable(sql, DialectType.MYSQL, databaseName2DataNodes);
        Set<DataNode> expected = new HashSet<>(Arrays.asList(
                new DataNode("db_0", "tb_0", 1L),
                new DataNode("db_1", "tb_1", 2L),
                new DataNode("db_2", "tb_2", 3L),
                new DataNode("db_3", "tb_3", 4L)));
        assertEquals(expected, actual);
    }

}
