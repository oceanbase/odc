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
package com.oceanbase.odc.plugin.connect.postgres;

import java.sql.Connection;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;

@Extension
public class PostgresInformationExtension implements InformationExtensionPoint {
    @Override
    public String getDBVersion(Connection connection) {
        String querySql = "SELECT current_setting('server_version');";
        try {
            return JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
        } catch (Exception e) {
            throw new BadRequestException(ErrorCodes.QueryDBVersionFailed,
                    new Object[] {"Result set is empty"}, "Result set is empty");
        }
    }
}
