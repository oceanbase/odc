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
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Implement for interface {@link SecuritySession}, framework use this implement to hold manage
 * session object. Caller can use this object to store some attributes
 *
 * @author yh263208
 * @date 2021-07-13 14:26
 * @since ODC_release_3.2.0
 * @see SecuritySession
 */
@Slf4j
public class DefaultSecuritySession implements SecuritySession, Serializable {

    private static final long serialVersionUID = -7125642695178165650L;
    private final Map<Object, Object> attributes = new HashMap<>();
    private final String id;
    private final Date startTime;
    private Date expiredTime;
    private Date lastAccessTime;
    private final long timeoutMilliSeconds;
    private boolean expired = false;
    private final String host;

    public DefaultSecuritySession() {
        this("0.0.0.0", SecurityConstants.DEFAULT_SESSION_TIMEOUT_MILLIS, null);
    }

    public DefaultSecuritySession(long timeoutMillSeconds) {
        this("0.0.0.0", timeoutMillSeconds, null);
    }

    public DefaultSecuritySession(@NonNull String host, long timeoutMillSeconds, String sessionId) {
        Validate.isTrue(timeoutMillSeconds > 0, "Timeout can not be negative");
        this.host = host;
        if (sessionId == null) {
            this.id = StringUtils.uuidNoHyphen();
        } else {
            this.id = sessionId;
        }
        long currentTimeStamp = System.currentTimeMillis();
        this.startTime = new Date(currentTimeStamp);
        this.lastAccessTime = new Date(currentTimeStamp);
        this.timeoutMilliSeconds = timeoutMillSeconds;
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    @Override
    public Date getStartTime() {
        return this.startTime;
    }

    @Override
    public Date getLastAccessTime() {
        return this.lastAccessTime;
    }

    @Override
    public boolean isExpired() {
        return this.expired;
    }

    @Override
    public long getTimeoutMillis() {
        return this.timeoutMilliSeconds;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public void touch() throws InvalidSessionException {
        validate();
        this.lastAccessTime = new Date();
    }

    @Override
    public void expire() {
        if (this.expiredTime == null) {
            this.expiredTime = new Date();
        }
        this.expired = true;
    }

    @Override
    public Collection<Object> getAttributeKeys() throws InvalidSessionException {
        return getAttributes().keySet();
    }

    @Override
    public Object getAttribute(Object key) throws InvalidSessionException {
        return getAttributes().get(key);
    }

    private Map<Object, Object> getAttributes() throws InvalidSessionException {
        validate();
        return this.attributes;
    }

    @Override
    public void setAttribute(Object key, Object value) throws InvalidSessionException {
        if (value == null) {
            this.removeAttribute(key);
        } else {
            getAttributes().put(key, value);
        }
    }

    @Override
    public Object removeAttribute(Object key) throws InvalidSessionException {
        return getAttributes().remove(key);
    }

    protected boolean isValid() {
        return this.expired;
    }

    protected boolean isTimedOut() {
        if (this.expired) {
            return true;
        } else {
            long timeout = this.getTimeoutMillis();
            if (timeout >= 0L) {
                Date lastAccessTime = this.getLastAccessTime();
                long expireTimeMillis = System.currentTimeMillis() - timeout;
                Date expireTime = new Date(expireTimeMillis);
                return lastAccessTime.before(expireTime);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No timeout for session with id [" + this.getId()
                            + "].  Session is not considered expired.");
                }
                return false;
            }
        }
    }

    protected void validate() throws InvalidSessionException {
        if (this.isTimedOut()) {
            this.expire();
            Date lastAccessTime = this.getLastAccessTime();
            long timeout = this.getTimeoutMillis();
            Serializable sessionId = this.getId();
            if (log.isDebugEnabled()) {
                DateFormat format = DateFormat.getInstance();
                log.debug("Session has expired, id={}, lastAccessTime={}, currentTime={}, timeout={} s ({} mins)",
                        sessionId, format.format(lastAccessTime), format.format(new Date()),
                        TimeUnit.SECONDS.convert(timeout, TimeUnit.MILLISECONDS),
                        TimeUnit.MINUTES.convert(timeout, TimeUnit.MILLISECONDS));
            }
            throw new InvalidSessionException(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof DefaultSecuritySession) {
            DefaultSecuritySession other = (DefaultSecuritySession) obj;
            Serializable thisId = this.getId();
            Serializable otherId = other.getId();
            return thisId.equals(otherId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

}
