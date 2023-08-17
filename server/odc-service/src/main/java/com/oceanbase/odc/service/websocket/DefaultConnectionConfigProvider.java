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

package com.oceanbase.odc.service.websocket;

import javax.websocket.Session;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.session.ConnectSessionService;

public class DefaultConnectionConfigProvider implements ConnectionConfigProvider {

    private static final ConnectionMapper CONNECTION_MAPPER = ConnectionMapper.INSTANCE;
    @Autowired
    private ConnectSessionService sessionService;

    @Override
    public ConnectionConfig getConnectionSession(String resourceId, Session session) {
        ConnectionSession s = sessionService.nullSafeGet(SidUtils.getSessionId(resourceId));
        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(s);
        return CONNECTION_MAPPER.clone(connectionConfig);
    }

}
