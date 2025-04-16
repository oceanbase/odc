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
package com.oceanbase.odc.migrate.jdbc.common;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;

import cn.hutool.core.codec.Caesar;

public class V4349OrganizationSecretMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OrganizationRepository organizationRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private String secret2;

    @Before
    public void init() {
        organizationRepository.deleteAll();

        this.jdbcTemplate = new JdbcTemplate(dataSource);
        String addTeamOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(100,'a','%s','OceanBase',1,0,'D','TEAM')";
        String secret = "Y75AZG91YuoepqL6VvyacJZ2fUaHVraI";
        jdbcTemplate.update(String.format(addTeamOrg, secret));
        String addIndivOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(1000,'b','%s','OceanBase2',1,0,'D','INDIVIDUAL')";
        // individual organization secret is encoded by BCryptPasswordEncoder
        secret2 = passwordEncoder.encode("aaAA11__");
        jdbcTemplate.update(String.format(addIndivOrg, secret2));
    }

    @After
    public void clear() {
        organizationRepository.deleteAll();
    }

    @Test
    public void teamOrganizationSecretMigrate() {
        V4349OrganizationSecretMigrate migrate = new V4349OrganizationSecretMigrate();
        migrate.migrate(dataSource);
        String migratedSecret = selectSecretFromOrganization(100L);
        String secret = Caesar.decode(migratedSecret, 8);
        Assert.assertEquals("Y75AZG91YuoepqL6VvyacJZ2fUaHVraI", secret);
    }

    @Test
    public void teamOrganizationSecretMigrate2() {
        String addTeamOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(101,'aa','%s','OceanBase3',1,0,'D','TEAM')";
        String currSecret = "AAAAZG91YuoepqL6VvyacJZ2fUaHVVVV";

        jdbcTemplate.update(String.format(addTeamOrg, currSecret));
        V4349OrganizationSecretMigrate migrate = new V4349OrganizationSecretMigrate();
        migrate.migrate(dataSource);
        String migratedSecret = selectSecretFromOrganization(101L);
        Assert.assertEquals(migratedSecret, Caesar.encode(currSecret, 8));
        String secret = Caesar.decode(migratedSecret, 8);
        Assert.assertEquals(currSecret, secret);
    }

    @Test
    public void individualOrganizationSecretMigrate() {
        V4349OrganizationSecretMigrate migrate = new V4349OrganizationSecretMigrate();
        migrate.migrate(dataSource);
        String migratedSecret = selectSecretFromOrganization(1000L);
        String secret = Caesar.decode(migratedSecret, 8);
        Assert.assertEquals(secret2, secret);
    }

    @Test
    public void individualOrganizationSecretMigrate2() {
        String addIndivOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(1001,'bb','%s','OceanBase4',1,0,'D','INDIVIDUAL')";
        String currSecret = passwordEncoder.encode("aaAA11___");
        jdbcTemplate.update(String.format(addIndivOrg, currSecret));

        V4349OrganizationSecretMigrate migrate = new V4349OrganizationSecretMigrate();
        migrate.migrate(dataSource);
        String migratedSecret = selectSecretFromOrganization(1001L);
        Assert.assertEquals(migratedSecret, Caesar.encode(currSecret, 8));
        String secret = Caesar.decode(migratedSecret, 8);
        Assert.assertEquals(currSecret, secret);
    }

    private String selectSecretFromOrganization(Long id) {
        String sql = "select `secret` from iam_organization where `id` = " + id;
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}
