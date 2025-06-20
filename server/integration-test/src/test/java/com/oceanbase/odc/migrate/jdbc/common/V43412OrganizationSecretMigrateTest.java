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

import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;

import cn.hutool.core.codec.Caesar;

public class V43412OrganizationSecretMigrateTest extends ServiceTestEnv {

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
                + "`id`,`unique_identifier`,`secret`,`secret_new`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(100,'a','%s','%s','OceanBase',1,0,'D','TEAM')";
        String secret = "Y75AZG91YuoepqL6VvyacJZ2fUaHVraI";
        jdbcTemplate.update(String.format(addTeamOrg, secret, secret));
        String addIndivOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`secret_new`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(1000,'b','%s','%s','OceanBase2',1,0,'D','INDIVIDUAL')";
        // individual organization secret is encoded by BCryptPasswordEncoder
        secret2 = passwordEncoder.encode("aaAA11__");
        jdbcTemplate.update(String.format(addIndivOrg, secret2, secret2));
    }

    @After
    public void clear() {
        organizationRepository.deleteAll();
    }

    @Test
    public void teamOrganizationSecretMigrate() {
        V43412OrganizationSecretMigrate migrate = new V43412OrganizationSecretMigrate();
        migrate.migrate(dataSource);
        String migratedSecret = selectSecretNewFromOrganization(100L);
        Assert.assertEquals(migratedSecret, Caesar.encode("Y75AZG91YuoepqL6VvyacJZ2fUaHVraI", 8));
        String secret = selectSecretFromOrganization(100L);
        Assert.assertEquals("Y75AZG91YuoepqL6VvyacJZ2fUaHVraI", secret);
        int count = selectAllFromOrganization().size();
        Assert.assertEquals(2, count);
    }

    @Test
    public void teamOrganizationSecretMigrate2() {
        String addTeamOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`secret_new`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(101,'aa','%s','%s','OceanBase3',1,0,'D','TEAM')";
        String currSecret = "AAAAZG91YuoepqL6VvyacJZ2fUaHVVVV";
        jdbcTemplate.update(String.format(addTeamOrg, currSecret, currSecret));

        V43412OrganizationSecretMigrate migrate = new V43412OrganizationSecretMigrate();
        migrate.migrate(dataSource);
        String migratedSecret = selectSecretNewFromOrganization(101L);
        Assert.assertEquals(migratedSecret, Caesar.encode(currSecret, 8));
        String secret = Caesar.decode(migratedSecret, 8);
        Assert.assertEquals(currSecret, secret);
        int count = selectAllFromOrganization().size();
        Assert.assertEquals(3, count);
    }

    @Test
    public void organizationSecretMigrate_AfterMigrate() {
        String addTeamOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`secret_new`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(102,'aaa','%s','%s','OB1',1,0,'D','TEAM')";
        String currSecret = "AAAAZG91YuoepqL6VvyacJZ2fUaHVVVV";
        jdbcTemplate.update(String.format(addTeamOrg, currSecret, currSecret));

        V43412OrganizationSecretMigrate migrate = new V43412OrganizationSecretMigrate();
        migrate.migrate(dataSource);

        String customSecret = selectSecretNewFromOrganization(100L);
        Assert.assertEquals(Caesar.encode("Y75AZG91YuoepqL6VvyacJZ2fUaHVraI", 8), customSecret);
        String secret = selectSecretFromOrganization(100L);
        Assert.assertEquals("Y75AZG91YuoepqL6VvyacJZ2fUaHVraI", secret);
        String customSecret2 = selectSecretNewFromOrganization(102L);
        Assert.assertEquals(Caesar.encode(currSecret, 8), customSecret2);
        String secret2 = selectSecretFromOrganization(102L);
        Assert.assertEquals(currSecret, secret2);
        int count = selectAllFromOrganization().size();
        Assert.assertEquals(3, count);
    }

    @Test
    public void individualOrganizationSecretMigrate() {
        V43412OrganizationSecretMigrate migrate = new V43412OrganizationSecretMigrate();
        migrate.migrate(dataSource);
        String migratedSecret = selectSecretNewFromOrganization(1000L);
        Assert.assertEquals(migratedSecret, Caesar.encode(this.secret2, 8));
        String secret = Caesar.decode(migratedSecret, 8);
        Assert.assertEquals(this.secret2, secret);
        int count = selectAllFromOrganization().size();
        Assert.assertEquals(2, count);
    }

    @Test
    public void individualOrganizationSecretMigrate2() {
        String addIndivOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`secret_new`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(1001,'bb','%s','%s','OceanBase4',1,0,'D','INDIVIDUAL')";
        String currSecret = passwordEncoder.encode("aaAA11___");
        jdbcTemplate.update(String.format(addIndivOrg, currSecret, currSecret));

        V43412OrganizationSecretMigrate migrate = new V43412OrganizationSecretMigrate();
        migrate.migrate(dataSource);
        String migratedSecret = selectSecretNewFromOrganization(1001L);
        Assert.assertEquals(migratedSecret, Caesar.encode(currSecret, 8));
        String secret = Caesar.decode(migratedSecret, 8);
        Assert.assertEquals(currSecret, secret);
        int count = selectAllFromOrganization().size();
        Assert.assertEquals(3, count);
    }

    private String selectSecretFromOrganization(Long id) {
        String sql = "select `secret` from iam_organization where `id` = " + id;
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private String selectSecretNewFromOrganization(Long id) {
        String sql = "select `secret_new` from iam_organization where `id` = " + id;
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private List<OrganizationEntity> selectAllFromOrganization() {
        String sql = "select `id`, `secret` from iam_organization";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(OrganizationEntity.class));
    }
}
