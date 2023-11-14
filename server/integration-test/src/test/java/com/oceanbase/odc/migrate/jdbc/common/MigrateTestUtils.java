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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

public class MigrateTestUtils {

    public static void clearUserAndOrganization(JdbcTemplate jdbcTemplate) {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "iam_user");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "iam_organization");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "iam_user_organization");
    }

    public static void initUserAndOrganization(JdbcTemplate jdbcTemplate, String organizationName, String accountName) {
        jdbcTemplate.update("insert into iam_organization(id, name, unique_identifier, secret, type)"
                + " values(1, ?, 'identifier', 'secret', 'INDIVIDUAL')", organizationName);
        jdbcTemplate.update("insert into iam_user(id, account_name, name, creator_id, organization_id, password) "
                + "values(1, ?, ?, 1, 1, 'password')", accountName, accountName);
        jdbcTemplate.update("insert into iam_user_organization(user_id, organization_id) values(1,1)");
    }
}
