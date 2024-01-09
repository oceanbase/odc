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

package com.oceanbase.odc.plugin.connect.oracle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pf4j.Extension;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;

/**
 * @author jingtian
 * @date 2023/11/8
 * @since ODC_release_4.2.4
 */
@Extension
public class OracleInformationExtension implements InformationExtensionPoint {
    private final String VERSION_REGEX = "\\b(\\d+(?:\\.\\d+)*)\\b";

    @Override
    public String getDBVersion(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("SELECT BANNER FROM V$VERSION WHERE BANNER LIKE 'Oracle%'")) {
                if (rs.next()) {
                    String banner = rs.getString(1);
                    Pattern pattern = java.util.regex.Pattern.compile(VERSION_REGEX);
                    Matcher matcher = pattern.matcher(banner);
                    if (matcher.find()) {
                        return matcher.group();
                    } else {
                        throw new BadRequestException(ErrorCodes.QueryDBVersionFailed,
                                new Object[] {"Failed to get oracle version,regular match failed"},
                                "Failed to get oracle version,regular match failed");
                    }
                } else {
                    throw new BadRequestException(ErrorCodes.QueryDBVersionFailed,
                            new Object[] {"Result set is empty"}, "Result set is empty");
                }
            }
        } catch (Exception e) {
            throw new BadRequestException(ErrorCodes.QueryDBVersionFailed,
                    new Object[] {e.getMessage()}, e.getMessage());
        }
    }
}
