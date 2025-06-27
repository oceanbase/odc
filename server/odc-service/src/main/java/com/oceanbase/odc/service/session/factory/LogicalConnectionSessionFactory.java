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
package com.oceanbase.odc.service.session.factory;

import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.session.ConnectionSessionIdGenerator;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.LogicalConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.CreateSessionReq;

import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/9 14:22
 * @Description: []
 */
@Slf4j
public class LogicalConnectionSessionFactory implements ConnectionSessionFactory {
    private final CreateSessionReq createSessionReq;
    private final ConnectType connectType;
    @Setter
    private long sessionTimeoutMillis;

    @Setter
    private ConnectionSessionIdGenerator<CreateSessionReq> idGenerator;

    public LogicalConnectionSessionFactory(@NotNull CreateSessionReq createSessionReq,
            @NotNull ConnectType connectType) {
        this.createSessionReq = createSessionReq;
        this.connectType = connectType;
        this.idGenerator = new DefaultConnectSessionIdGenerator();
        this.sessionTimeoutMillis = TimeUnit.MILLISECONDS.convert(
                ConnectionSessionConstants.SESSION_EXPIRATION_TIME_SECONDS, TimeUnit.SECONDS);
    }


    @Override
    public ConnectionSession generateSession() {
        try {
            ConnectionSession connectionSession =
                    new LogicalConnectionSession(idGenerator.generateId(this.createSessionReq), this.connectType,
                            this.sessionTimeoutMillis);
            ConnectionSessionUtil.setLogicalSession(connectionSession, Boolean.TRUE);
            return connectionSession;
        } catch (Exception e) {
            log.warn("Failed to create connection session", e);
            throw new IllegalStateException(e);
        }
    }
}
