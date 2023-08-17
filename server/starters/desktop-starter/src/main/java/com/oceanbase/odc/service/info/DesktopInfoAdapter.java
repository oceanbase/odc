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

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;

@Service
@Profile("clientMode")
@SkipAuthorize("odc internal usage")
public class DesktopInfoAdapter implements InfoAdapter {

    @Value("${odc.help.supportGroupQRCodeUrl:#{null}}")
    private String supportGroupQRCodeUrl;

    @Override
    public boolean isPasswordLoginEnabled() {
        return false;
    }

    @Override
    public String getLoginUrl(HttpServletRequest request) {
        return null;
    }

    @Override
    public String getLogoutUrl(HttpServletRequest request) {
        return null;
    }

    @Override
    public String getSupportGroupQRCodeUrl() {
        return supportGroupQRCodeUrl;
    }

}
