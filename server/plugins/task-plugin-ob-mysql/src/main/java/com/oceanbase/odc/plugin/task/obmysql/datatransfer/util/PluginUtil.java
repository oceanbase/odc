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

package com.oceanbase.odc.plugin.task.obmysql.datatransfer.util;

import java.util.Objects;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.connect.mysql.MySQLConnectionExtension;
import com.oceanbase.odc.plugin.connect.mysql.MySQLInformationExtension;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLConnectionExtension;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLInformationExtension;
import com.oceanbase.odc.plugin.connect.oboracle.OBOracleConnectionExtension;
import com.oceanbase.odc.plugin.connect.oboracle.OBOracleInformationExtension;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.schema.mysql.MySQLTableExtension;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.odc.plugin.schema.oboracle.OBOracleTableExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;

public class PluginUtil {

    public static InformationExtensionPoint getInformationExtension(DataTransferConfig config) {
        DialectType dialectType = getDialectType(config);
        if (dialectType == DialectType.OB_MYSQL) {
            return new OBMySQLInformationExtension();
        } else if (dialectType == DialectType.OB_ORACLE) {
            return new OBOracleInformationExtension();
        } else {
            return new MySQLInformationExtension();
        }
    }

    public static ConnectionExtensionPoint getConnectionExtension(DataTransferConfig config) {
        DialectType dialectType = getDialectType(config);
        if (dialectType == DialectType.OB_MYSQL) {
            return new OBMySQLConnectionExtension();
        } else if (dialectType == DialectType.OB_ORACLE) {
            return new OBOracleConnectionExtension();
        } else {
            return new MySQLConnectionExtension();
        }
    }

    public static TableExtensionPoint getTableExtension(DataTransferConfig config) {
        DialectType dialectType = getDialectType(config);
        if (dialectType == DialectType.OB_MYSQL) {
            return new OBMySQLTableExtension();
        } else if (dialectType == DialectType.OB_ORACLE) {
            return new OBOracleTableExtension();
        } else {
            return new MySQLTableExtension();
        }
    }

    private static DialectType getDialectType(DataTransferConfig config) {
        ConnectionInfo connectionInfo = Objects.requireNonNull(config.getConnectionInfo());
        return connectionInfo.getConnectType().getDialectType();
    }

}
