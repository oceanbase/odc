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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.execute.AsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.GeneralAsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.GeneralSyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SessionOperations;
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
public class DefaultConnectionSession extends AbstractConnectionSession {

    private final boolean defaultAutoCommit;
    private final Map<String, DataSourceWrapper> dataSourceWrapperMap;
    private final TaskManagerWrapper taskManagerWrapper;
    private final SessionOperations sessionOperations;
    private BinaryDataManager dataManager;

    public DefaultConnectionSession(@NonNull String id,
            TaskManagerFactory<SqlExecuteTaskManager> taskManagerFactory, long sessionTimeoutMillis,
            @NonNull ConnectType connectType, boolean defaultAutoCommit,
            @NonNull SessionOperations sessionOperations) throws IOException {
        super(id, connectType, sessionTimeoutMillis);
        this.defaultAutoCommit = defaultAutoCommit;
        if (taskManagerFactory == null) {
            this.taskManagerWrapper = new TaskManagerWrapper(
                    () -> new DefaultSqlExecuteTaskManager(1, "console", 10, TimeUnit.SECONDS));
        } else {
            this.taskManagerWrapper = new TaskManagerWrapper(taskManagerFactory);
        }
        this.dataSourceWrapperMap = new HashMap<>();
        initBinaryDataManager();
        this.sessionOperations = sessionOperations;
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
    public boolean getDefaultAutoCommit() {
        return this.defaultAutoCommit;
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
                taskManager, sessionOperations);
    }

    private DataSourceWrapper getDataSourceWrapper(@NonNull String dataSourceName) {
        validate();
        DataSourceWrapper wrapper = this.dataSourceWrapperMap.get(dataSourceName);
        if (wrapper == null) {
            throw new IllegalStateException("Can not find datasource by name " + dataSourceName);
        }
        return wrapper;
    }

    @Override
    protected void closeDataSource() {
        for (String key : this.dataSourceWrapperMap.keySet()) {
            DataSource dataSource = this.dataSourceWrapperMap.get(key).dataSource;
            if (!(dataSource instanceof AutoCloseable)) {
                continue;
            }
            try {
                ((AutoCloseable) dataSource).close();
                log.info("Datasource is closed successfully, name={}, sessionId={}", key, this.id);
            } catch (Exception e) {
                log.warn("Failed to close dataSource, name={}, sessionId={}, error={}",
                        key, this.id, ExceptionUtils.getRootCauseReason(e));
            }
        }
    }

    @Override
    protected void closeTaskManager() {
        SqlExecuteTaskManager taskManager = this.taskManagerWrapper.taskManager;
        if (taskManager == null) {
            return;
        }
        try {
            taskManager.close();
            if (log.isDebugEnabled()) {
                log.debug("TaskManager is closed successfully, sessionId={}", this.id);
            }
        } catch (Exception e) {
            log.warn("Failed to close the task manager, sessionId={}, error={}",
                    this.id, ExceptionUtils.getRootCauseReason(e));
        }
    }

    @Override
    protected void closeBinaryFileManager() {
        if (this.dataManager == null) {
            return;
        }
        try {
            this.dataManager.close();
            if (log.isDebugEnabled()) {
                log.debug("Binary data manager closed successfully, sessionId={}", this.id);
            }
        } catch (Exception e) {
            log.warn("Binary file manager shutdown failed, sessionId={}, manager={}, error={}",
                    this.id, this.dataManager, ExceptionUtils.getRootCauseReason(e));
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
