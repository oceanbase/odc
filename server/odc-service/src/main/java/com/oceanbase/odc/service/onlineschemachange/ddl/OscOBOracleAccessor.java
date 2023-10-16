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

package com.oceanbase.odc.service.onlineschemachange.ddl;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @author yaobin
 * @date 2023-10-13
 * @since 4.2.3
 */
public class OscOBOracleAccessor implements OscDBAccessor {

    protected final JdbcOperations jdbcOperations;

    public OscOBOracleAccessor(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public List<DBUser> listUsers(List<String> usernames) {
        SqlBuilder sb = new OracleSqlBuilder();
        // all_users in oceanbase do not provide account locked status.
        // so we query locked info from SYS.ALL_VIRTUAL_USER_REAL_AGENT
        sb.append("SELECT user_name, is_locked FROM SYS.ALL_VIRTUAL_USER_REAL_AGENT");
        if (CollectionUtils.isNotEmpty(usernames)) {
            sb.append(" WHERE user_name IN (");
            sb.values(usernames);
            sb.append(")");
        }
        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBUser dbUser = new DBUser();
            dbUser.setName(rs.getString(1));
            dbUser.setAccountLocked(DBAccountLockType.from(rs.getInt(2)));
            return dbUser;
        });
    }
}
