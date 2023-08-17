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
package com.oceanbase.odc.core.authority.session.validate;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionValidateManager;
import com.oceanbase.odc.core.shared.Verify;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SecuritySessionValidateTask}
 *
 * @author yh263208
 * @date 2022-10-28 16:41
 * @since ODC_release_4.0.1
 */
@Slf4j
public class SecuritySessionValidateTask implements Runnable {

    private final List<Predicate<SecuritySession>> validators;
    private final long sessionScanIntervalMillis;
    private final SecuritySessionValidateManager sessionManager;

    public SecuritySessionValidateTask(@NonNull SecuritySessionValidateManager sessionManager,
            @NonNull List<Predicate<SecuritySession>> validators, long scanIntervalMillis) {
        this.sessionManager = sessionManager;
        this.validators = validators;
        Validate.isTrue(scanIntervalMillis > 0, "ScanInterval can not be negative");
        this.sessionScanIntervalMillis = scanIntervalMillis;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Collection<SecuritySession> sessions = sessionManager.retrieveAllSessions();
                Verify.notNull(sessions, "Sessions");
                SecuritySessionValidateTask that = this;
                sessions.stream().filter(s -> that.validators.stream().anyMatch(p -> !p.test(s))).forEach(session -> {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Session failed to pass verification, sessionId={}", session.getId());
                        }
                        sessionManager.removeCertainSession(session);
                    } catch (Exception e) {
                        log.warn("Failed to remove a session from manager, sessionId={}", session.getId(), e);
                    }
                });
                Thread.sleep(sessionScanIntervalMillis);
            } catch (InterruptedException e) {
                log.warn("Refresh thread has been interrupted", e);
                return;
            } catch (Exception e) {
                log.warn("Refresh daemon thread has been Encountered an exception and will try again", e);
            }
        }
    }

}
