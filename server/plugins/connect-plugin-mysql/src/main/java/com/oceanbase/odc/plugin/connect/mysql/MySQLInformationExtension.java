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
package com.oceanbase.odc.plugin.connect.mysql;

import java.sql.Connection;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/5/26
 * @since ODC_release_4.2.0
 */
@Slf4j
@Extension
public class MySQLInformationExtension implements InformationExtensionPoint {

    @Override
    public String getDBVersion(Connection connection) {
        String querySql = "show variables like 'innodb_version'";
        String dbVersion;
        try {
            dbVersion = JdbcOperationsUtil.getJdbcOperations(connection).query(querySql, rs -> {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString(2);
            });
        } catch (Exception e) {
            throw new BadRequestException(ErrorCodes.QueryDBVersionFailed,
                    new Object[] {e.getMessage()}, e.getMessage());
        }
        if (dbVersion == null) {
            throw new BadRequestException(ErrorCodes.QueryDBVersionFailed,
                    new Object[] {"Result set is empty"}, "Result set is empty");
        }
        return parseDBVersion(dbVersion);
    }

    private String parseDBVersion(String version) {
        if (version.contains("-")) {
            return version.split("-")[0];
        }
        return version;
    }

}
