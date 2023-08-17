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

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;

/**
 * @author yaobin
 * @date 2023-06-10
 * @since 4.2.0
 */
public class DdlUtils {

    private static final Pattern NAME_QUOTE_PATTERN = Pattern.compile("^\"(.+)\"$");
    private static final Pattern NAME_ACCENT_PATTERN = Pattern.compile("^`(.+)`$");

    public static String getNewTableName(String rawName) {

        return getNewNameWithSuffix(rawName, DdlConstants.NEW_TABLE_NAME_SUFFIX);
    }

    public static String getRenamedTableName(String rawName) {

        return getNewNameWithSuffix(rawName, DdlConstants.RENAMED_TABLE_NAME_SUFFIX);
    }

    public static String getNewNameWithSuffix(String rawName, String suffix) {
        if (rawName == null) {
            return null;
        }
        return DdlConstants.OSC_TABLE_NAME_PREFIX + getUnwrappedName(rawName) + suffix;
    }

    public static String queryOriginTableCreateDdl(ConnectionSession session, String tableName)
            throws SQLException {
        SyncJdbcExecutor syncJdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        String query = "show create table " + tableName;
        List<String> ddl = syncJdbcExecutor.query(query, (rs, num) -> rs.getString(2));
        if (CollectionUtils.isEmpty(ddl) || StringUtils.isBlank(ddl.get(0))) {
            throw new SQLException(String.format("Failed to get ddl for table %s", tableName));
        }
        return ddl.get(0);
    }

    public static String getUnwrappedName(String rawName) {
        Matcher matcher = NAME_QUOTE_PATTERN.matcher(rawName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        matcher = NAME_ACCENT_PATTERN.matcher(rawName);

        return matcher.matches() ? matcher.group(1) : rawName;
    }

    public static String replaceTableName(String sql, String newTableName, DialectType dialectType,
            OnlineSchemaChangeSqlType sqlType) {
        TableNameReplacer rewriter =
                dialectType.isMysql() ? new OBMysqlTableNameReplacer() : new OBOracleTableNameReplacer();
        return sqlType == OnlineSchemaChangeSqlType.CREATE ? rewriter.replaceCreateStmt(sql, newTableName)
                : rewriter.replaceAlterStmt(sql, newTableName);
    }

    public static String getUUIDWithoutUnderline() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
