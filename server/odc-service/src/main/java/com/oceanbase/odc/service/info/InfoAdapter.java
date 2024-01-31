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
package com.oceanbase.odc.service.info;

import java.time.OffsetDateTime;

import javax.servlet.http.HttpServletRequest;

public interface InfoAdapter {

    String getLoginUrl(HttpServletRequest request);

    String getLogoutUrl(HttpServletRequest request);

    String getSupportGroupQRCodeUrl();

    String getBuildVersion();

    OffsetDateTime getBuildTime();

    boolean isPasswordLoginEnabled();

    default boolean isSSoLoginEnabled(HttpServletRequest request) {
        return getLoginUrl(request) != null;
    }

    default String ssoLoginName() {
        return "";
    }

    default String ssoLoginType() {
        return "";
    }
}
