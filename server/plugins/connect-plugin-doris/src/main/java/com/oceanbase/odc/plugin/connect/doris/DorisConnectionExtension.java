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
package com.oceanbase.odc.plugin.connect.doris;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.mysql.MySQLConnectionExtension;

import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: DorisConnectionExtension Package: com.oceanbase.odc.plugin.connect.doris Description:
 *
 * @Author: fenghao
 * @Create 2024/1/4 17:07
 * @Version 1.0
 */
@Slf4j
@Extension
public class DorisConnectionExtension extends MySQLConnectionExtension {
    private final String MIN_VERSION_SUPPORTED = "5.7.0";

    @Override
    public TestResult test(String jdbcUrl, Properties properties, int queryTimeout) {
        // fix arbitrary file reading vulnerability
        properties.setProperty("allowLoadLocalInfile", "false");
        properties.setProperty("allowUrlInLocalInfile", "false");
        properties.setProperty("allowLoadLocalInfileInPath", "");
        properties.setProperty("autoDeserialize", "false");
        TestResult testResult = super.internalTest(jdbcUrl, properties, queryTimeout);
        if (testResult.getErrorCode() != null) {
            return testResult;
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl, properties)) {
            DorisInformationExtension informationExtension = new DorisInformationExtension();
            String version = informationExtension.getDBVersion(connection);
            if (VersionUtils.isLessThan(version, MIN_VERSION_SUPPORTED)) {
                return TestResult.unsupportedDBVersion(version);
            }
        } catch (Exception e) {
            return TestResult.unknownError(e);
        }
        return testResult;
    }


}
