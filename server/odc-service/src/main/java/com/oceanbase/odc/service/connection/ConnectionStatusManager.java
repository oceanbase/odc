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
package com.oceanbase.odc.service.connection;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.ConnectionStatus;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.service.common.SystemTimeService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Fetch connection status in async mode, for performance purpose
 * 
 * @author yizhou.xw
 * @version : ConnectionStatusManager.java, v 0.1 2021-07-28 13:09
 */
@Slf4j
@Service
public class ConnectionStatusManager {

    @Value("${odc.connect.statusCheckIntervalSeconds:120}")
    private long statusCheckIntervalSeconds = 120L;

    @Value("${odc.connect.statusCheckKeepSeconds:600}")
    private long statusCheckKeepSeconds = 600L;

    @Value("${odc.connect.removeExpiredIntervalSeconds:60}")
    private long removeExpiredIntervalSeconds = 60L;

    @Autowired
    private ConnectionTesting connectionTesting;
    @Autowired
    private SystemTimeService systemTimeService;
    @Autowired
    private ConnectionEncryption connectionEncryption;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    @Qualifier("connectionStatusCheckExecutor")
    public ThreadPoolTaskExecutor statusCheckExecutor;

    private final Map<CheckKey, CheckState> connect2State = new ConcurrentHashMap<>();
    private volatile long nextRemoveExpiredTimeMillis = 0;

    CheckState getAndRefreshStatus(ConnectionConfig connection) {
        PreConditions.notNull(connection, "connection");
        if (Objects.nonNull(connection.getEnabled()) && !connection.getEnabled()) {
            return CheckState.of(ConnectionStatus.DISABLED);
        }
        CheckKey checkKey = new CheckKey(connection);
        CheckState checkState = connect2State.computeIfAbsent(checkKey, t -> new CheckState());
        long currentTimeMillis = systemTimeService.currentTimeMillis();
        synchronized (checkState) {
            if ((checkState.nextCheckTimeMillis <= currentTimeMillis)
                    && (statusCheckExecutor.getMaxPoolSize() - statusCheckExecutor.getActiveCount() > 0)) {
                statusCheckExecutor.submit(new CheckTask(connection, checkState));
            }
            checkState.lastAccessTimeMillis = currentTimeMillis;
        }
        removeExpired(currentTimeMillis);
        return checkState;
    }

    /**
     * for UT
     */
    void clear() {
        connect2State.clear();
    }

    private void removeExpired(long currentTimeMillis) {
        if (currentTimeMillis < nextRemoveExpiredTimeMillis) {
            return;
        }
        synchronized (connect2State) {
            if (currentTimeMillis < nextRemoveExpiredTimeMillis) {
                return;
            }
            nextRemoveExpiredTimeMillis = currentTimeMillis + removeExpiredIntervalSeconds * 1000L;
            int before = connect2State.size();
            connect2State.entrySet().removeIf(entry -> {
                CheckState checkState = entry.getValue();
                return checkState.lastAccessTimeMillis + statusCheckKeepSeconds * 1000L < currentTimeMillis;
            });
            int after = connect2State.size();
            if (before != after) {
                log.info("Remove expired, currentTimeMillis={}, before={}, after={}",
                        currentTimeMillis, before, after);
            }
        }
    }

    @EqualsAndHashCode
    private class CheckKey {

        long id;
        long updateTimeMills;

        private CheckKey(ConnectionConfig connection) {
            Verify.notNull(connection.getCreateTime(), "connection.createTime");
            this.id = connection.getId();
            this.updateTimeMills = (Objects.isNull(connection.getUpdateTime())
                    ? connection.getCreateTime()
                    : connection.getUpdateTime()).getTime();
        }
    }

    @Getter
    public static class CheckState implements Serializable {

        @JsonIgnore
        private String[] args;
        private ErrorCode errorCode;
        private ConnectionStatus status;
        private ConnectType type;
        @Getter(AccessLevel.NONE)
        private long nextCheckTimeMillis;
        @Getter(AccessLevel.NONE)
        private long lastAccessTimeMillis;

        public CheckState() {
            this.status = ConnectionStatus.TESTING;
            this.nextCheckTimeMillis = 0L;
            this.lastAccessTimeMillis = System.currentTimeMillis();
        }

        public static CheckState of(@NonNull ConnectionStatus status) {
            CheckState checkState = new CheckState();
            checkState.status = status;
            return checkState;
        }

        public void refresh(@NonNull ConnectionTestResult result) {
            this.status = result.isActive() ? ConnectionStatus.ACTIVE : ConnectionStatus.INACTIVE;
            this.type = result.getType();
            this.errorCode = result.getErrorCode();
            this.args = result.getArgs();
        }

        public String getErrorMessage() {
            if (this.errorCode == null || this.args == null) {
                return null;
            }
            return this.errorCode.getLocalizedMessage(this.args);
        }
    }

    private class CheckTask implements Callable<ConnectionTestResult> {
        final TestConnectionReq testConnectionReq;
        final CheckState checkState;
        final User user;

        CheckTask(ConnectionConfig connection, CheckState checkState) {
            this.testConnectionReq = TestConnectionReq.fromConnection(connection, ConnectionAccountType.MAIN);
            this.user = authenticationFacade.currentUser();
            if (Objects.isNull(connection.getPassword())) {
                try {
                    TextEncryptor encryptor = connectionEncryption.getEncryptor(connection);
                    this.testConnectionReq.setPassword(encryptor.decrypt(connection.getPasswordEncrypted()));
                } catch (Exception e) {
                    log.warn("Test connection decrypt password failed, connectionId={}, reason={}",
                            connection.getId(), e.getMessage());
                }
            }
            this.checkState = checkState;
        }

        @Override
        public ConnectionTestResult call() {
            checkState.nextCheckTimeMillis = systemTimeService.currentTimeMillis() + statusCheckIntervalSeconds * 1000L;
            ConnectionTestResult result;
            Long connectionId = this.testConnectionReq.getId();
            try {
                SecurityContextUtils.setCurrentUser(this.user);
                result = connectionTesting.test(this.testConnectionReq);
            } catch (Exception e) {
                result = new ConnectionTestResult(TestResult.unknownError(e), null);
                log.info("Test connection failed, connectionId={}, reason={}, rootCause={}",
                        connectionId, e.getMessage(), ExceptionUtils.getRootCauseReason(e));
            } finally {
                SecurityContextUtils.clear();
            }
            this.checkState.refresh(result);
            return result;
        }
    }
}
