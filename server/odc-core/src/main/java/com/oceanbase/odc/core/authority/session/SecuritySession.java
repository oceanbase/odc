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

/**
 * Session object, the authority system needs to have a session management mechanism to identify
 * users
 *
 * @author yh263208
 * @date 2021-07-12 19:19
 * @since ODC_release_3.2.0
 */
public interface SecuritySession {
    /**
     * The unqiue ID for session. Session object used this ID to store itself
     *
     * @return unqiue ID
     */
    Serializable getId();

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
     * Get timeout milli-seconds
     *
     * @return timeout milli-seconds
     */
    long getTimeoutMillis();

    /**
     * Get the remote host value created by the session
     *
     * @return host value
     */
    String getHost();

    /**
     * Reset the expiration time of the session object
     *
     * @exception InvalidSessionException exception will be thrown when session is already expired
     */
    void touch() throws InvalidSessionException;

    /**
     * Force the session object to expire
     */
    void expire();

    /**
     * Get the keys of all attribute objects on the session object
     *
     * @return collections of attribute keys
     * @exception InvalidSessionException exception will be thrown when session is already expired
     */
    Collection<Object> getAttributeKeys() throws InvalidSessionException;

    /**
     * Get the value of an attribute bound to the session object
     *
     * @param key key for an attribute
     * @return attribute value
     * @exception InvalidSessionException exception will be thrown when session is already expired
     */
    Object getAttribute(Object key) throws InvalidSessionException;

    /**
     * Assign a value to an attribute on the session object
     *
     * @param key key for an attribute
     * @param value value for an attribute
     * @exception InvalidSessionException exception will be thrown when session is already expired
     */
    void setAttribute(Object key, Object value) throws InvalidSessionException;

    /**
     * Remove an attribute on the session object
     *
     * @param key key for an attribute that will be deleted
     * @return deleted attribute value
     * @exception InvalidSessionException exception will be thrown when session is already expired
     */
    Object removeAttribute(Object key) throws InvalidSessionException;

}
