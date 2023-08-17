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
import java.util.Map;

import com.oceanbase.odc.core.authority.session.factory.SecuritySessionFactory;

/**
 * There are many events in the life cycle of a {@link SecuritySession}, such as
 * {@link SecuritySession} creation, {@link SecuritySession} deletion, {@link SecuritySession}
 * update acquisition and invalidation, and so on. The event listener is to monitor these events.
 *
 * @author yh263208
 * @date 2021-07-14 20:05
 * @since ODC_release_3.2.0
 */
public interface SecuritySessionEventListener {
    /**
     * Method will be called when a session has been created by {@link SecuritySessionFactory} and
     * stored by session {@link SecuritySessionRepository}
     *
     * @param session {@link SecuritySession} object that has been created and stored
     */
    void onCreateEventSucceed(SecuritySession session, Map<String, Object> context);

    /**
     * Method will be called when a session has been created by {@link SecuritySessionFactory} and
     * stored by session {@link SecuritySessionRepository}
     *
     * @param session {@link SecuritySession} object that has been created and stored
     * @param e exception will be thrown when create session failed
     */
    void onCreateEventFailed(SecuritySession session, Map<String, Object> context, Throwable e);

    /**
     * Method will be called when a {@link SecuritySession} has been deleted from
     * {@link SecuritySessionManager}
     *
     * @param session {@link SecuritySession} object that has been deleted
     * @param id Unique ID for {@link SecuritySession}
     */
    void onDeleteEventSucceed(Serializable id, SecuritySession session);

    /**
     * Method will be called when a {@link SecuritySession} has been deleted from
     * {@link SecuritySessionManager}
     *
     * @param id Unique ID for {@link SecuritySession}
     * @param e exception will be thrown when create session failed
     */
    void onDeleteEventFailed(Serializable id, Throwable e);

    /**
     * Method will be called when a {@link SecuritySession} object has been updated by
     * {@link SecuritySessionRepository}
     *
     * @param session {@link SecuritySession} object that has been updated
     * @param id Unique ID for {@link SecuritySession}
     */
    void onUpdateEventSucceed(Serializable id, SecuritySession session);

    /**
     * Method will be called when a {@link SecuritySession} object has been updated by
     * {@link SecuritySessionRepository}
     *
     * @param session {@link SecuritySession} object that has been updated
     * @param id Unique ID for {@link SecuritySession}
     */
    void onUpdateEventFailed(Serializable id, SecuritySession session, Throwable e);

    /**
     * Method will be called when a {@link SecuritySession} has been achieved by a user.
     *
     * @param session {@link SecuritySession} object that has been achieved
     * @param id Unique ID for {@link SecuritySession}
     */
    void onGetEventSucceed(Serializable id, SecuritySession session);

    /**
     * Method will be called when a {@link SecuritySession} has been achieved by a user.
     *
     * @param id Unique ID for {@link SecuritySession}
     */
    void onGetEventFailed(Serializable id, Throwable e);

    /**
     * Method will be called when a {@link SecuritySession} has been expired by a user.
     *
     * @param session {@link SecuritySession} object that has been achieved
     * @param id Unique ID for {@link SecuritySession}
     */
    void onExpiredEventSucceed(Serializable id, SecuritySession session);

    /**
     * Method will be called when a {@link SecuritySession} has been expired by a user.
     *
     * @param session {@link SecuritySession} object that has been achieved
     * @param id Unique ID for {@link SecuritySession}
     */
    void onExpiredEventFailed(Serializable id, SecuritySession session, Throwable e);

}
