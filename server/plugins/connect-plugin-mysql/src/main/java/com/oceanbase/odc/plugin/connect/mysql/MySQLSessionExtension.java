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

import com.oceanbase.odc.plugin.connect.model.DBClientInfo;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLSessionExtension;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/5/26
 * @since ODC_release_4.2.0
 */
@Slf4j
@Extension
public class MySQLSessionExtension extends OBMySQLSessionExtension {
    @Override
    public boolean setClientInfo(Connection connection, DBClientInfo clientInfo) {
        return false;
    }

}
