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
package com.oceanbase.odc.core.authority.session;

import java.io.Serializable;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Useless session event listener, because there are too many events in the session, and users often
 * don’t care about all the events, so set up a useless session event listener to provide empty
 * implementations for each method.
 *
 * If necessary, users can directly Just inherit the event listener and override the required
 * method.
 *
 * @author yh263208
 * @date 2021-07-15 15:42
 * @since ODC_release_3.2.0
 * @see SecuritySessionEventListener
 */
@Slf4j
public class EmptySecuritySessionEventListener implements SecuritySessionEventListener {

    @Override
    public void onCreateEventSucceed(SecuritySession session, Map<String, Object> context) {
        if (log.isDebugEnabled()) {
            log.debug("Create s session successfully, sessionId={}", session.getId());
        }
    }

    @Override
    public void onCreateEventFailed(SecuritySession session, Map<String, Object> context, Throwable e) {
        log.warn("Failed to create a session", e);
    }

    @Override
    public void onDeleteEventSucceed(Serializable id, SecuritySession session) {
        if (log.isDebugEnabled()) {
            log.debug("Delete a session successfully, sessionId={}", session.getId());
        }
    }

    @Override
    public void onDeleteEventFailed(Serializable id, Throwable e) {
        log.warn("Failed to delete a session", e);
    }

    @Override
    public void onUpdateEventSucceed(Serializable id, SecuritySession session) {
        if (log.isDebugEnabled()) {
            log.debug("Update a session successfully, sessionId={}", session.getId());
        }
    }

    @Override
    public void onUpdateEventFailed(Serializable id, SecuritySession session, Throwable e) {
        log.warn("Failed to update a session", e);
    }

    @Override
    public void onGetEventSucceed(Serializable id, SecuritySession session) {
        if (log.isDebugEnabled()) {
            log.debug("Get a session successfully, sessionId={}", session.getId());
        }
    }

    @Override
    public void onGetEventFailed(Serializable id, Throwable e) {
        log.warn("Failed to get a session", e);
    }

    @Override
    public void onExpiredEventSucceed(Serializable id, SecuritySession session) {
        if (log.isDebugEnabled()) {
            log.debug("Expire a session successfully, sessionId={}", session.getId());
        }
    }

    @Override
    public void onExpiredEventFailed(Serializable id, SecuritySession session, Throwable e) {
        log.warn("Failed to expire a session", e);
    }

}
