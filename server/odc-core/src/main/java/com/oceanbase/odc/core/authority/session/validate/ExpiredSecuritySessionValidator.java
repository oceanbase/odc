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

import java.util.function.Predicate;

import com.oceanbase.odc.core.authority.session.SecuritySession;

import lombok.extern.slf4j.Slf4j;

/**
 * Validator used to detect expired sessions
 *
 * @author yh263208
 * @date 2021-07-15 11:47
 * @see Predicate
 * @since ODC_release_3.2.0
 */
@Slf4j
public class ExpiredSecuritySessionValidator implements Predicate<SecuritySession> {

    @Override
    public boolean test(SecuritySession session) {
        if (session == null) {
            throw new IllegalStateException("session is null");
        }
        long timestamp = System.currentTimeMillis();
        long lastAccessTime = session.getLastAccessTime().getTime();
        long interval = timestamp - lastAccessTime;
        if (interval <= session.getTimeoutMillis() && !session.isExpired()) {
            return true;
        }
        log.warn("Session invalid, id={}, lastAccessTime={}, timeout={} millis",
                session.getId(), session.getLastAccessTime(), session.getTimeoutMillis());
        return false;
    }

}
