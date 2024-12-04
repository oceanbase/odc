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
package com.oceanbase.odc.service.pldebug.operator;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DBPLOperators {

    public static DBPLOperator create(ConnectionSession connectionSession) {
        PreConditions.notNull(connectionSession, "connectionSession");

        ConnectType connectType = connectionSession.getConnectType();
        PreConditions.notNull(connectType, "connectType");
        String obVersion = ConnectionSessionUtil.getVersion(connectionSession);
        PreConditions.notNull(obVersion, "obVersion");

        if (connectType == ConnectType.OB_ORACLE) {
            return new OraclePLOperator(connectionSession);
        } else if (connectType == ConnectType.CLOUD_OB_ORACLE) {
            String obProxyVersion = getObProxyVersion(connectionSession);
            if (obProxyVersion != null && VersionUtils.isGreaterThanOrEqualsTo(obProxyVersion, "3.1.11")) {
                return new OraclePLOperator(connectionSession);
            } else {
                throw new UnsupportedException(String.format(
                        "ODP version not supported, the version number must be greater than or equal to 3.1.11"));
            }
        } else {
            throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }

    /**
     * Get the OBProxy version number. If an exception occurs or the version does not support, return
     * null.
     *
     * @param connectionSession
     * @return
     */
    public static String getObProxyVersion(@NonNull ConnectionSession connectionSession) {
        try {
            return connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                    .queryForObject("select proxy_version()", String.class);
        } catch (Exception e) {
            log.warn("Failed to obtain the OBProxy version number: {}", e.getMessage());
            return null;
        }
    }
}
