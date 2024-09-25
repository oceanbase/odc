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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/20 14:25
 * @Description: []
 */
@Slf4j
public abstract class AbstractConnectionSession implements ConnectionSession {
    protected final String id;
    protected final ConnectType connectType;
    protected final long sessionTimeoutMillis;
    protected final Date startTime;
    protected boolean expired = false;
    protected Date expiredTime;
    protected Date lastAccessTime;
    protected final Map<Object, Object> attributes;

    public AbstractConnectionSession(@NonNull String id, @NonNull ConnectType connectType, long sessionTimeoutMillis)
            throws IOException {
        this.id = id;
        this.connectType = connectType;
        long timestamp = System.currentTimeMillis();
        this.startTime = new Date(timestamp);
        this.lastAccessTime = new Date(timestamp);
        this.sessionTimeoutMillis = sessionTimeoutMillis;
        this.attributes = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ConnectType getConnectType() {
        return this.connectType;
    }

    @Override
    public DialectType getDialectType() {
        return this.connectType.getDialectType();
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
    public synchronized void expire() {
        if (log.isDebugEnabled()) {
            log.debug("Connection session started to be expired, session={}", this);
        }
        if (this.expiredTime == null) {
            this.expiredTime = new Date();
        }
        this.expired = true;
        closeTaskManager();
        closeDataSource();
        closeBinaryFileManager();
        File sessionLevelDir = null;
        try {
            sessionLevelDir = ConnectionSessionUtil.getSessionWorkingDir(this);
            if (sessionLevelDir.exists()) {
                FileUtils.forceDelete(sessionLevelDir);
            }
            if (log.isDebugEnabled()) {
                log.debug("Session-level storage directory was deleted successfully, sessionId={}, dir={}",
                        this.id, sessionLevelDir);
            }
        } catch (IOException exception) {
            log.warn("Failed to delete session level directory, dir={}, sessionId={}",
                    sessionLevelDir, this.id, exception);
        }
        log.info("Connection session was closed successfully, sessionId={}", this.id);
    }

    @Override
    public void touch() throws ExpiredSessionException {
        validate();
        this.lastAccessTime = new Date();
    }

    @Override
    public long getTimeoutMillis() {
        return sessionTimeoutMillis;
    }

    @Override
    public Collection<Object> getAttributeKeys() throws ExpiredSessionException {
        return getAttributes().keySet();
    }

    @Override
    public Object getAttribute(Object key) throws ExpiredSessionException {
        return getAttributes().get(key);
    }

    @Override
    public void setAttribute(Object key, Object value) throws ExpiredSessionException {
        if (value == null) {
            this.removeAttribute(key);
        } else {
            getAttributes().put(key, value);
        }
    }

    @Override
    public Object removeAttribute(Object key) throws ExpiredSessionException {
        return getAttributes().remove(key);
    }

    private Map<Object, Object> getAttributes() throws ExpiredSessionException {
        validate();
        return this.attributes;
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

    protected void validate() throws ExpiredSessionException {
        if (this.isTimedOut()) {
            Date lastAccessTime = this.getLastAccessTime();
            long timeout = this.getTimeoutMillis();
            Serializable sessionId = this.getId();
            if (log.isDebugEnabled()) {
                DateFormat format = DateFormat.getInstance();
                log.debug(
                        "Connection Session has expired, id={}, lastAccessTime={}, currentTime={}, timeout={} s ({} mins)",
                        sessionId, format.format(lastAccessTime), format.format(new Date()),
                        TimeUnit.SECONDS.convert(timeout, TimeUnit.MILLISECONDS),
                        TimeUnit.MINUTES.convert(timeout, TimeUnit.MILLISECONDS));
            }
            throw new ExpiredSessionException(this);
        }
    }


    protected abstract void closeDataSource();

    protected abstract void closeTaskManager();

    protected abstract void closeBinaryFileManager();

}
