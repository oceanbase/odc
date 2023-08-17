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

/**
 * There are many events in the life cycle of a {@link ConnectionSession}, such as
 * {@link ConnectionSession} creation, {@link ConnectionSession} deletion, {@link ConnectionSession}
 * update acquisition and invalidation, and so on. The event listener is to monitor these events.
 *
 * @author yh263208
 * @date 2021-11-15 20:15
 * @since ODC_release_3.2.2
 */
public interface ConnectionSessionEventListener {
    /**
     * Method will be called when a session has been created by {@link ConnectionSessionFactory} and
     * stored by session {@link ConnectionSessionRepository}
     *
     * @param session {@link ConnectionSession} object that has been created and stored
     */
    void onCreateSucceed(ConnectionSession session);

    /**
     * Method will be called when a session has been created by {@link ConnectionSessionFactory} and
     * stored by session {@link ConnectionSessionRepository}
     *
     * @param session {@link ConnectionSession} object that has been created and stored
     * @param e exception will be thrown when create session failed
     */
    void onCreateFailed(ConnectionSession session, Throwable e);

    /**
     * Method will be called when a {@link ConnectionSession} has been deleted from
     * {@link ConnectionSessionManager}
     *
     * @param session {@link ConnectionSession} object that has been deleted
     */
    void onDeleteSucceed(ConnectionSession session);

    /**
     * Method will be called when a {@link ConnectionSession} has been deleted from
     * {@link ConnectionSessionManager}
     *
     * @param id Unique ID for {@link ConnectionSession}
     * @param e exception will be thrown when create session failed
     */
    void onDeleteFailed(String id, Throwable e);

    /**
     * Method will be called when a {@link ConnectionSession} has been achieved by a user.
     *
     * @param session {@link ConnectionSession} object that has been achieved
     */
    void onGetSucceed(ConnectionSession session);

    /**
     * Method will be called when a {@link ConnectionSession} has been achieved by a user.
     *
     * @param id Unique ID for {@link ConnectionSession}
     */
    void onGetFailed(String id, Throwable e);

    /**
     * Method will be called when a {@link ConnectionSession} expire by a user.
     *
     * @param session {@link ConnectionSession} object that has been achieved
     */
    void onExpire(ConnectionSession session);

    /**
     * Method will be called when a {@link ConnectionSession} has been expired by a user.
     *
     * @param session {@link ConnectionSession} object that has been achieved
     */
    void onExpireSucceed(ConnectionSession session);

    /**
     * Method will be called when a {@link ConnectionSession} has been expired by a user.
     *
     * @param session {@link ConnectionSession} object that has been achieved
     */
    void onExpireFailed(ConnectionSession session, Throwable e);

}
