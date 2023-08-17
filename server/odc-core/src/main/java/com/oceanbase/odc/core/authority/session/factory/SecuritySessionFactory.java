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
package com.oceanbase.odc.core.authority.session.factory;

import java.util.Map;

import com.oceanbase.odc.core.authority.session.SecuritySession;

/**
 * Factory to make {@link SecuritySession} object Caller can use some parameters to init a
 * {@link SecuritySession}, such as host ip or timeout seconds Factory will use these parameter to
 * init a {@link SecuritySession} object
 *
 * @author yh263208
 * @date 2021-07-13 17:55
 * @since ODC_release_3.2.0
 */
public interface SecuritySessionFactory {
    /**
     * Method to create a new {@link SecuritySession}, caller have to give a init parameter to init this
     * {@link SecuritySession} object
     *
     * @param initParameter init parameter
     * @return {@link SecuritySession} object
     */
    SecuritySession createSession(Map<String, Object> initParameter);

}
