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
package com.oceanbase.odc.core.session;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.shared.Verify;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Connection session verification task, used to verify the validity of the connection session and
 * remove invalid sessions
 *
 * @author yh263208
 * @date 2021-11-15 21:27
 * @since ODC-release_3.2.2
 * @see java.lang.Runnable
 */
@Slf4j
public class ConnectionSessionValidateTask implements Runnable {

    private final ValidatedConnectionSessionManager sessionManager;
    private final long sessionScanIntervalMillis;
    private final List<Predicate<ConnectionSession>> validatePredicates;

    public ConnectionSessionValidateTask(@NonNull ValidatedConnectionSessionManager sessionManager,
            @NonNull List<Predicate<ConnectionSession>> validatePredicates, long intervalMillis) {
        this.sessionManager = sessionManager;
        this.validatePredicates = validatePredicates;
        Validate.isTrue(intervalMillis > 0, "SessionScanIntervalMillis can not be negative");
        this.sessionScanIntervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Collection<ConnectionSession> sessions = sessionManager.retrieveAllSessions();
                Verify.notNull(sessions, "Sessions");
                ConnectionSessionValidateTask that = this;
                sessions.stream().filter(s -> that.validatePredicates.stream().anyMatch(p -> !p.test(s)))
                        .forEach(session -> {
                            try {
                                log.info("Session failed to pass verification, session={}", session);
                                session.expire();
                            } catch (Exception e) {
                                log.warn("Failed to remove a session from manager, sessionId={}", session.getId(), e);
                            }
                        });
                Thread.sleep(sessionScanIntervalMillis);
            } catch (InterruptedException e) {
                log.warn("Validate connection session task is interrupted, task exit", e);
                return;
            } catch (Exception e) {
                log.warn("Failed to validate and refresh sessions from all", e);
            }
        }
    }

}
