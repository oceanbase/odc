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
package com.oceanbase.odc.core.migrate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class SchemaHistoryRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:test;MODE=MySQL";

    private SchemaHistoryRepository repository;
    private Timestamp now = Timestamp.from(Instant.now());
    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        dataSource = new SingleConnectionDataSource(JDBC_URL, false);
        repository = new SchemaHistoryRepository("migrate_schema_history", dataSource);
    }

    @After
    public void tearDown() {
        new JdbcTemplate(dataSource).execute("drop table migrate_schema_history");
    }

    @Test
    public void listAll_NoRecord_Empty() {
        List<SchemaHistory> list = repository.listAll();

        Assert.assertEquals(0, list.size());
    }

    @Test
    public void listAll_OneRecord_Match() {
        SchemaHistory history = generate();
        repository.create(history);
        SchemaHistory expected1 = generate();
        expected1.setInstallRank(1L);
        List<SchemaHistory> expected = Arrays.asList(expected1);

        List<SchemaHistory> list = repository.listAll();

        Assert.assertArrayEquals(expected.toArray(), list.toArray());
    }

    @Test
    public void listSuccess_HasOneFailed_SizeWas_0() {
        SchemaHistory history = generate();
        history.setSuccess(false);
        repository.create(history);

        List<SchemaHistory> list = repository.listSuccess();

        Assert.assertEquals(0, list.size());
    }

    @Test
    public void create_First_InstallRankWas_1() {
        SchemaHistory history = generate();

        SchemaHistory schemaHistory = repository.create(history);

        Assert.assertEquals(1L, schemaHistory.getInstallRank().longValue());
    }

    private SchemaHistory generate() {
        SchemaHistory history = new SchemaHistory();
        history.setVersion("1.0.0");
        history.setDescription("script bla bla");
        history.setChecksum("123456");
        history.setSuccess(true);
        history.setExecutionMillis(1L);
        history.setInstalledOn(now);
        history.setInstalledBy("test");
        history.setType(Type.SQL);
        history.setScript("test.sql");
        return history;
    }

    @Test
    public void currentUser_NoUser_Empty() {
        String username = repository.currentUser();
        Assert.assertEquals("", username);
    }
}
