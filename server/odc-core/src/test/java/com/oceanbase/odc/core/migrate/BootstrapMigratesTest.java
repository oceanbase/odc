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

import java.util.Arrays;
import java.util.Collections;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class BootstrapMigratesTest {
    private static final String JDBC_URL = "jdbc:h2:mem:test;MODE=MySQL";

    private DataSource dataSource;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        dataSource = new SingleConnectionDataSource(JDBC_URL, false);
    }

    @After
    public void tearDown() {
        new JdbcTemplate(dataSource).execute("drop table if exists migrate_schema_history");
        new JdbcTemplate(dataSource).execute("drop table if exists t_1_for_migrate_test");
        new JdbcTemplate(dataSource).execute("drop table if exists t_2_for_migrate_test");
    }

    @Test
    public void migrate_Success() {
        MigrateConfiguration configuration = MigrateConfiguration.builder()
                .dataSource(dataSource)
                .resourceLocations(Collections.singletonList("migrate/migrate"))
                .basePackages(Arrays.asList("com.oceanbase.odc.core.migrate"))
                .build();

        BootstrapMigrates migrates = new BootstrapMigrates(configuration);

        migrates.migrate();

        Long rowCount =
                new JdbcTemplate(dataSource).queryForObject("select count(*) from migrate_schema_history", Long.class);

        Assert.assertEquals(3L, rowCount.longValue());
    }

    @Test
    public void migrate_InitVersionEqualsMax_SkipExecuteButFillHistory() {
        // init then clear history
        MigrateConfiguration init = MigrateConfiguration.builder()
                .dataSource(dataSource)
                .resourceLocations(Collections.singletonList("migrate/migrate"))
                .basePackages(Arrays.asList("com.oceanbase.odc.core.migrate"))
                .build();
        new BootstrapMigrates(init).migrate();
        new JdbcTemplate(dataSource).execute("drop table migrate_schema_history");

        MigrateConfiguration second = MigrateConfiguration.builder()
                .dataSource(dataSource)
                .resourceLocations(Collections.singletonList("migrate/migrate"))
                .basePackages(Arrays.asList("com.oceanbase.odc.core.migrate"))
                .initVersion("1.3.2")
                .build();

        new BootstrapMigrates(second).migrate();
        Long rowCount = new JdbcTemplate(dataSource)
                .queryForObject("select count(*) from migrate_schema_history where script like '%V%'", Long.class);

        Assert.assertEquals(2L, rowCount.longValue());
    }

    @Test
    public void migrate_BasePackageContainsNoImplement_Exception() {
        MigrateConfiguration configuration = MigrateConfiguration.builder()
                .dataSource(dataSource)
                .resourceLocations(Collections.singletonList("migrate/migrate"))
                .basePackages(Arrays.asList("com.oceanbase.odc.core.migrate.no.impl"))
                .build();

        thrown.expectMessage("no JdbcMigratable implementation found under basePackages");

        BootstrapMigrates migrates = new BootstrapMigrates(configuration);
        migrates.migrate();
    }

    @Test
    public void migrate_Degrade_Exception() {
        MigrateConfiguration configuration = MigrateConfiguration.builder()
                .dataSource(dataSource)
                .resourceLocations(Collections.singletonList("migrate/migrate"))
                .basePackages(Arrays.asList("com.oceanbase.odc.core.migrate"))
                .build();

        // prepare
        SchemaHistoryRepository repository =
                new SchemaHistoryRepository(BootstrapMigrates.DEFAULT_TABLE, configuration.getDataSource());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO `migrate_schema_history` (`install_rank`,`version`,`description`,`type`,`script`,`checksum`,"
                        + "`installed_by`,`installed_on`,`execution_millis`,`success`) VALUES (1,'2.0.0','init','SQL','V_2_0_0__init"
                        + ".sql','92a77bfee81f5c9e087b75f3152d6553a21f281b','odc@%','2022-03-25 15:22:00.0',550,1);");
        jdbcTemplate.update(
                "INSERT INTO `migrate_schema_history` (`install_rank`,`version`,`description`,`type`,`script`,`checksum`,"
                        + "`installed_by`,`installed_on`,`execution_millis`,`success`) VALUES (16,'3.2.0','enterprise console "
                        + "schema','SQL','V_3_2_0__enterprise_console_schema.sql','d66ad1cc468778968b3597be58a0926e48428579','odc@%',"
                        + "'2022-03-25 15:22:06.0',1031,1);");
        jdbcTemplate.update(
                "INSERT INTO `migrate_schema_history` (`install_rank`,`version`,`description`,`type`,`script`,`checksum`,"
                        + "`installed_by`,`installed_on`,`execution_millis`,`success`) VALUES (31,'3.4.0','add audit event and meta',"
                        + "'SQL','V_3_3_0__add_audit_event_and_meta.sql','5020e1e6e3f1174d036e9c048d6c38d5770f6131','odc@%',"
                        + "'2022-03-25 15:22:09.0',166,1);");

        thrown.expectMessage(
                "Software degrade is not allowed, please check your ODC version which should be greater than or equal to 3.4.0");

        BootstrapMigrates migrates = new BootstrapMigrates(configuration);
        migrates.migrate();
    }

}
