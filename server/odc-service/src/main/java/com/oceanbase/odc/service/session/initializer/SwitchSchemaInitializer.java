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
package com.oceanbase.odc.service.session.initializer;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SwitchSchemaInitializer} used to modify current schema
 *
 * @author yh263208
 * @date 2022-10-11 10:46
 * @since ODC_release_3.5.0
 * @see ConnectionInitializer
 */
@Slf4j
public class SwitchSchemaInitializer implements ConnectionInitializer {

    private final ConnectionSession connectionSession;

    public SwitchSchemaInitializer(@NonNull ConnectionSession connectionSession) {
        this.connectionSession = connectionSession;
    }

    @Override
    public void init(Connection connection) throws SQLException {
        if (this.connectionSession.isExpired()) {
            return;
        }
        String currentSchema = ConnectionSessionUtil.getCurrentSchema(this.connectionSession);
        String connectSchema = ConnectionSessionUtil.getConnectSchema(this.connectionSession);
        if (currentSchema == null || StringUtils.equals(connectSchema, currentSchema)) {
            return;
        }
        ConnectionPluginUtil.getSessionExtension(connectionSession.getDialectType())
                .switchSchema(connection, currentSchema);
    }

}
