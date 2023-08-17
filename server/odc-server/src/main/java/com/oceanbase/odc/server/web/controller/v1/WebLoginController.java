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
package com.oceanbase.odc.server.web.controller.v1;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.info.OdcInfoService;

@RestController
@RequestMapping("/api/v1/")
public class WebLoginController {
    public static final String TO_BE_REPLACED = "TO_BE_REPLACED";

    @Autowired
    private OdcInfoService odcInfoService;

    @GetMapping("sso-login")
    public SuccessResponse<String> ssoLogin(HttpServletRequest request) {
        String loginUrl = odcInfoService.getLoginUrl();
        if (cannotRedirect(loginUrl)) {
            return Responses.ok(null);
        }
        return Responses.ok(OdcInfoService.addOdcParameter(request, loginUrl));
    }

    @GetMapping("sso-logout")
    public SuccessResponse<String> ssoLogout(HttpServletRequest request) {
        String logoutUrl = odcInfoService.getLogoutUrl();
        if (cannotRedirect(logoutUrl)) {
            logoutUrl = odcInfoService.getLoginUrl();
        }
        if (cannotRedirect(logoutUrl)) {
            return Responses.ok(null);
        }
        return Responses.ok(OdcInfoService.addOdcParameter(request, logoutUrl));
    }

    private boolean cannotRedirect(String url) {
        return Objects.isNull(url) || TO_BE_REPLACED.equals(url);
    }

}
