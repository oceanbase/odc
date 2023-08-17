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
import java.util.Date;

import javax.sql.DataSource;

import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.AsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;

import lombok.NonNull;

/**
 * The class is used to represent a session object when a user open a connection
 *
 * @author yh263208
 * @date 2021-11-01 20:34
 * @since ODC_release_3.2.2
 */
public interface ConnectionSession {
    /**
     * The unqiue ID for session. Session object used this ID to store itself
     *
     * @return unqiue ID
     */
    String getId();

    /**
     * Put {@link DataSource} to {@link ConnectionSession}
     *
     * @param name name of the {@link DataSource}, can not be duplicated
     * @param dataSourceFactory {@link DataSourceFactory}
     */
    void register(@NonNull String name, @NonNull DataSourceFactory dataSourceFactory);

    /**
     * Get connect type
     *
     * @return {@link ConnectType}
     */
    ConnectType getConnectType();

    /**
     * Get dialect type value
     *
     * @return {@link DialectType}
     */
    DialectType getDialectType();

    /**
     * auto commit settings for connection
     *
     * @return commit settings
     */
    boolean getDefaultAutoCommit();

    /**
     * The creation time of the session object
     *
     * @return create time
     */
    Date getStartTime();

    /**
     * Last access time of the session object
     *
     * @return last update time
     */
    Date getLastAccessTime();

    /**
     * Flag to indicate whether the session is expired
     *
     * @return flag value
     */
    boolean isExpired();

    /**
     * Force the session object to expire
     */
    void expire();

    /**
     * Reset the expiration time of the session object
     *
     * @exception ExpiredSessionException exception will be thrown when session is already expired
     */
    void touch() throws ExpiredSessionException;

    /**
     * Get timeout milli-seconds
     *
     * @return timeout milli-seconds
     */
    long getTimeoutMillis();

    /**
     * Get the keys of all attribute objects on the session object
     *
     * @return collections of attribute keys
     */
    Collection<Object> getAttributeKeys() throws ExpiredSessionException;

    /**
     * Get the value of an attribute bound to the session object
     *
     * @param key key for an attribute
     * @return attribute value
     */
    Object getAttribute(Object key) throws ExpiredSessionException;

    /**
     * Assign a value to an attribute on the session object
     *
     * @param key key for an attribute
     * @param value value for an attribute
     */
    void setAttribute(Object key, Object value) throws ExpiredSessionException;

    /**
     * Remove an attribute on the session object
     *
     * @param key key for an attribute that will be deleted
     * @return deleted attribute value
     */
    Object removeAttribute(Object key) throws ExpiredSessionException;

    /**
     * Get synchronous jdbc executor
     *
     * @param dataSourceName name of the {@link DataSource}
     * @return {@link SyncJdbcExecutor}
     * @throws ExpiredSessionException {@link ConnectionSession} may be expired
     */
    SyncJdbcExecutor getSyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException;

    /**
     * Get asynchronous jdbc executor
     *
     * @param dataSourceName name of the {@link DataSource}
     * @return {@link AsyncJdbcExecutor}
     * @throws ExpiredSessionException {@link ConnectionSession} may be expired
     */
    AsyncJdbcExecutor getAsyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException;

}
