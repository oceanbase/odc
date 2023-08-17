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

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.AsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.ConnectionExtensionExecutor;
import com.oceanbase.odc.core.sql.execute.GeneralAsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.GeneralSyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.FileBaseBinaryDataManager;
import com.oceanbase.odc.core.sql.execute.task.DefaultSqlExecuteTaskManager;
import com.oceanbase.odc.core.sql.execute.task.SqlExecuteTaskManager;
import com.oceanbase.odc.core.task.TaskManagerFactory;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Default database connection session implementation
 *
 * @author yh263208
 * @date 2021-11-15 16:08
 * @since ODC_release_3.2.2
 * @see ConnectionSession
 */
@Slf4j
@ToString(exclude = {"attributes", "dataSourceWrapperMap", "taskManagerWrapper"})
@EqualsAndHashCode(exclude = {"attributes", "dataSourceWrapperMap", "taskManagerWrapper", "dataManager"})
public class DefaultConnectionSession implements ConnectionSession {

    private final String id;
    private final ConnectType connectType;
    private final boolean defaultAutoCommit;
    private final Date startTime;
    private final Map<String, DataSourceWrapper> dataSourceWrapperMap;
    private final long sessionTimeoutMillis;
    private final Map<Object, Object> attributes;
    private final TaskManagerWrapper taskManagerWrapper;

    private Date lastAccessTime;
    private boolean expired = false;
    private Date expiredTime;
    private BinaryDataManager dataManager;
    private ConnectionExtensionExecutor extensionExecutor;

    public DefaultConnectionSession(@NonNull ConnectionSessionIdGenerator idGenerator,
            TaskManagerFactory<SqlExecuteTaskManager> taskManagerFactory, long sessionTimeoutMillis,
            @NonNull ConnectType connectType, boolean defaultAutoCommit,
            @NonNull ConnectionExtensionExecutor extensionExecutor) throws IOException {
        this.id = idGenerator.generateId();
        this.connectType = connectType;
        this.defaultAutoCommit = defaultAutoCommit;
        long timestamp = System.currentTimeMillis();
        this.startTime = new Date(timestamp);
        this.lastAccessTime = new Date(timestamp);
        this.sessionTimeoutMillis = sessionTimeoutMillis;
        if (taskManagerFactory == null) {
            this.taskManagerWrapper = new TaskManagerWrapper(
                    () -> new DefaultSqlExecuteTaskManager(1, "console", 10, TimeUnit.SECONDS));
        } else {
            this.taskManagerWrapper = new TaskManagerWrapper(taskManagerFactory);
        }
        this.attributes = new HashMap<>();
        this.dataSourceWrapperMap = new HashMap<>();
        initBinaryDataManager();
        this.extensionExecutor = extensionExecutor;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void register(@NonNull String name, @NonNull DataSourceFactory dataSourceFactory) {
        if (this.dataSourceWrapperMap.containsKey(name)) {
            throw new IllegalArgumentException("Duplicated name, " + name);
        }
        this.dataSourceWrapperMap.put(name, new DataSourceWrapper(dataSourceFactory));
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
    public boolean getDefaultAutoCommit() {
        return this.defaultAutoCommit;
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
    public void expire() {
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
            log.warn("Failed to delete session level directory, dir={}", sessionLevelDir, exception);
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

    @Override
    public SyncJdbcExecutor getSyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        DataSourceWrapper wrapper = getDataSourceWrapper(dataSourceName);
        return new GeneralSyncJdbcExecutor(wrapper.getOrCreateDataSource());
    }

    @Override
    public AsyncJdbcExecutor getAsyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        DataSourceWrapper wrapper = getDataSourceWrapper(dataSourceName);
        DataSourceFactory factory = wrapper.dataSourceFactory;
        if (!(factory instanceof CloneableDataSourceFactory)) {
            throw new IllegalStateException("Type is illegal, actual is " + factory);
        }
        SqlExecuteTaskManager taskManager = this.taskManagerWrapper.getOrCreateTaskManager();
        CloneableDataSourceFactory dataSourceFactory = (CloneableDataSourceFactory) factory;
        return new GeneralAsyncJdbcExecutor(wrapper.getOrCreateDataSource(), dataSourceFactory,
                taskManager, extensionExecutor);
    }

    private DataSourceWrapper getDataSourceWrapper(@NonNull String dataSourceName) {
        validate();
        DataSourceWrapper wrapper = this.dataSourceWrapperMap.get(dataSourceName);
        if (wrapper == null) {
            throw new IllegalStateException("Can not find datasource by name " + dataSourceName);
        }
        return wrapper;
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
            this.expire();
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

    private void closeDataSource() {
        for (String key : this.dataSourceWrapperMap.keySet()) {
            DataSource dataSource = this.dataSourceWrapperMap.get(key).dataSource;
            if (!(dataSource instanceof AutoCloseable)) {
                continue;
            }
            try {
                ((AutoCloseable) dataSource).close();
                log.info("Datasource is closed successfully, name={}", key);
            } catch (Exception e) {
                log.warn("Failed to close dataSource, name={}", key, e);
            }
        }
    }

    private void closeTaskManager() {
        SqlExecuteTaskManager taskManager = this.taskManagerWrapper.taskManager;
        if (taskManager == null) {
            return;
        }
        try {
            taskManager.close();
        } catch (Exception e) {
            log.warn("Failed to close task manager", e);
        }
    }

    private void closeBinaryFileManager() {
        if (this.dataManager == null) {
            return;
        }
        try {
            this.dataManager.close();
            if (log.isDebugEnabled()) {
                log.debug("Binary data manager closed successfully, sessionId={}", this.id);
            }
        } catch (Exception e) {
            log.warn("Binary file manager shutdown failed, sessionId={}, manager={}", this.id, this.dataManager, e);
        }
    }

    private void initBinaryDataManager() throws IOException {
        File workingDir = ConnectionSessionUtil.getSessionDataManagerDir(this);
        this.dataManager = new FileBaseBinaryDataManager(workingDir.getAbsolutePath());
        ConnectionSessionUtil.setBinaryDataManager(this, this.dataManager);
        log.info("Init binary data manager completed.");
    }

    static class DataSourceWrapper {

        private final Object lockObject = new Object();
        private final DataSourceFactory dataSourceFactory;
        private volatile DataSource dataSource;

        DataSourceWrapper(@NonNull DataSourceFactory dataSourceFactory) {
            this.dataSourceFactory = dataSourceFactory;
        }

        DataSource getOrCreateDataSource() {
            if (this.dataSource == null) {
                synchronized (this.lockObject) {
                    if (this.dataSource == null) {
                        this.dataSource = this.dataSourceFactory.getDataSource();
                    }
                }
            }
            return this.dataSource;
        }
    }

    static class TaskManagerWrapper {

        private final Object lockObject = new Object();
        private final TaskManagerFactory<SqlExecuteTaskManager> taskManagerFactory;
        private volatile SqlExecuteTaskManager taskManager;

        TaskManagerWrapper(@NonNull TaskManagerFactory<SqlExecuteTaskManager> taskManagerFactory) {
            this.taskManagerFactory = taskManagerFactory;
        }

        SqlExecuteTaskManager getOrCreateTaskManager() {
            if (this.taskManager == null) {
                synchronized (this.lockObject) {
                    if (this.taskManager == null) {
                        this.taskManager = this.taskManagerFactory.generateManager();
                    }
                }
            }
            return this.taskManager;
        }
    }

}
