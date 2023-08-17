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

import lombok.extern.slf4j.Slf4j;

/**
 * Empty implemention for {@link ConnectionSessionEventListener}
 *
 * @author yh263208
 * @date 2021-11-15 20:46
 * @since ODC_release_3.2.2
 * @see ConnectionSessionEventListener
 */
@Slf4j
public class DefaultSessionEventListener implements ConnectionSessionEventListener {

    @Override
    public void onCreateSucceed(ConnectionSession session) {
        if (log.isDebugEnabled()) {
            log.debug("Create s session successfully, session={}", session);
        }
    }

    @Override
    public void onCreateFailed(ConnectionSession session, Throwable e) {
        log.warn("Fail to create a session, session={}", session, e);
    }

    @Override
    public void onDeleteSucceed(ConnectionSession session) {
        if (log.isDebugEnabled()) {
            log.debug("Delete a session successfully, sessionId={}", session.getId());
        }
    }

    @Override
    public void onDeleteFailed(String id, Throwable e) {
        log.warn("Fail to delete a session, sessionId={}", id, e);
    }

    @Override
    public void onGetSucceed(ConnectionSession session) {
        if (log.isDebugEnabled()) {
            log.debug("Get a session successfully, sessionId={}", session.getId());
        }
    }

    @Override
    public void onGetFailed(String id, Throwable e) {
        log.warn("Fail to get a session, sessionId={}", id, e);
    }

    @Override
    public void onExpire(ConnectionSession session) {
        if (log.isDebugEnabled()) {
            log.debug("Expire a session, session={}", session);
        }
    }

    @Override
    public void onExpireSucceed(ConnectionSession session) {
        if (log.isDebugEnabled()) {
            log.debug("Expire a session successfully, session={}", session);
        }
    }

    @Override
    public void onExpireFailed(ConnectionSession session, Throwable e) {
        log.warn("Fail to expire a session, session={}", session, e);
    }

}
