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
package com.oceanbase.odc.plugin.schema.obmysql.browser;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.OBMySQLNoLessThan400StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.OBMySQLStatsAccessor;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/28
 * @since 4.2.0
 */
public class DBStatsAccessors {
    public static DBStatsAccessor create(@NonNull JdbcOperations jdbcOperations, @NonNull String dbVersion) {
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "4.0.0")) {
            // OB 版本 >= 4.0.0
            return new OBMySQLNoLessThan400StatsAccessor(jdbcOperations);
        } else {
            return new OBMySQLStatsAccessor(jdbcOperations);
        }
    }
}
