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
package com.oceanbase.odc.server.web.highavailable;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.oceanbase.odc.config.CommonSecurityProperties;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.common.response.Error;
import com.oceanbase.odc.service.common.response.ErrorResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimitBucketProvider rateLimitBucketProvider;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private CommonSecurityProperties commonSecurityProperties;
    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }
        String requestURI = request.getRequestURI();
        boolean urlInAuthWhiteList = Arrays.stream(commonSecurityProperties.getAuthWhitelist())
                .anyMatch(url -> StringUtils.containsIgnoreCase(requestURI, url));
        if (urlInAuthWhiteList) {
            return true;
        }
        boolean urlInWhiteList = Arrays.stream(rateLimitProperties.getUrlWhiteList())
                .anyMatch(url -> StringUtils.containsIgnoreCase(requestURI, url));
        if (urlInWhiteList) {
            return true;
        }

        String userId = authenticationFacade.currentUserIdStr();
        Bucket bucket = rateLimitBucketProvider.resolveApiBucket(userId);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            ErrorResponse errorResponse =
                    Responses.error(HttpStatus.TOO_MANY_REQUESTS,
                            Error.of(ErrorCodes.TooManyRequest, new Object[] {waitForRefill}));
            WebResponseUtils.writeJsonObjectWithStatus(errorResponse, request, response,
                    HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
    }
}
