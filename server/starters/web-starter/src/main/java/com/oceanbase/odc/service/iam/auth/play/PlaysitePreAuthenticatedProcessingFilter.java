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
package com.oceanbase.odc.service.iam.auth.play;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.LocaleResolver;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.iam.auth.AbstractOdcPreAuthenticatedProcessingFilter;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/12/23 下午4:57
 * @Description: [Authenticate by Alipay OpenAPI]
 */
@Slf4j
public class PlaysitePreAuthenticatedProcessingFilter extends AbstractOdcPreAuthenticatedProcessingFilter {
    private final PlaysiteOpenAPIClient alipayOpenAPIClient;

    public PlaysitePreAuthenticatedProcessingFilter(PlaysiteOpenAPIClient alipayOpenAPIClient,
            @NonNull SecurityManager securityManager,
            LocaleResolver localeResolver) {
        super(securityManager, localeResolver);
        this.alipayOpenAPIClient = alipayOpenAPIClient;
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        String obAuthToken =
                WebRequestUtils.getCookieValue(request, PlaysiteOpenAPIConstants.OB_OFFICIAL_WEBSITE_TOKEN_COOKIE_NAME);
        if (com.oceanbase.odc.common.util.StringUtils.isEmpty(obAuthToken)) {
            log.info("Null or empty 'authorization' value from http request, request url={}",
                    request.getRequestURL());
            throw new AccessDeniedException(ErrorCodes.LoginExpired, "please re-login from OceanBase official website");
        }
        PlaysiteUser obOfficialWebsiteUser;
        try {
            obOfficialWebsiteUser = alipayOpenAPIClient.acquireObUserInfo(obAuthToken);
        } catch (Exception ex) {
            log.warn("Call OpenAPI for acquiring ob user info failed, errMsg={}", ex.getMessage());
            throw new AccessDeniedException(ErrorCodes.LoginExpired, ex.getMessage());
        }
        return obOfficialWebsiteUser.getPassportId();
    }

}
