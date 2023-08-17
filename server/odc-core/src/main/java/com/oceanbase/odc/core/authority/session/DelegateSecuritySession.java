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
import java.util.Collection;
import java.util.Date;

import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.session.manager.BaseSecuritySessionManager;

import lombok.NonNull;

/**
 * The delgate object of the {@link SecuritySession}, the main purpose of setting the changed object
 * is to monitor events on the {@link SecuritySession} object
 *
 * @author yh263208
 * @date 2021-07-14 20:47
 * @since ODC_release_3.2.0
 */
public class DelegateSecuritySession implements SecuritySession {

    private final SecuritySession delgate;
    private final BaseSecuritySessionManager sessionManager;

    public DelegateSecuritySession(@NonNull BaseSecuritySessionManager sessionManager,
            @NonNull SecuritySession session) {
        this.sessionManager = sessionManager;
        this.delgate = session;
    }

    @Override
    public Serializable getId() {
        return this.sessionManager.getId(delgate);
    }

    @Override
    public Date getStartTime() {
        return this.sessionManager.getStartTime(delgate);
    }

    @Override
    public Date getLastAccessTime() {
        return this.sessionManager.getLastAccessTime(delgate);
    }

    @Override
    public boolean isExpired() {
        return this.sessionManager.isExpired(delgate);
    }

    @Override
    public long getTimeoutMillis() {
        return this.sessionManager.getTimeoutMillis(delgate);
    }

    @Override
    public String getHost() {
        return this.sessionManager.getHost(delgate);
    }

    @Override
    public void touch() throws InvalidSessionException {
        this.sessionManager.touch(delgate);
    }

    @Override
    public void expire() {
        this.sessionManager.expire(delgate);
    }

    @Override
    public Collection<Object> getAttributeKeys() throws InvalidSessionException {
        return this.sessionManager.getAttributeKeys(delgate);
    }

    @Override
    public Object getAttribute(Object key) throws InvalidSessionException {
        return this.sessionManager.getAttribute(delgate, key);
    }

    @Override
    public void setAttribute(Object key, Object value) throws InvalidSessionException {
        this.sessionManager.setAttribute(delgate, key, value);
    }

    @Override
    public Object removeAttribute(Object key) throws InvalidSessionException {
        return this.sessionManager.removeAttribute(delgate, key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof DelegateSecuritySession) {
            DelegateSecuritySession other = (DelegateSecuritySession) obj;
            Serializable thisId = this.delgate.getId();
            Serializable otherId = other.delgate.getId();
            return thisId.equals(otherId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.delgate.getId().hashCode();
    }

}
