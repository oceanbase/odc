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
package com.oceanbase.odc.service.integration.oauth2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.info.OdcInfoService;

import lombok.SneakyThrows;

@Component
public class Oauth2StateManager {

    private final Cache<String, Map<String, String>> STATE_PARAM_CACHE = CacheBuilder.newBuilder().maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Autowired
    private RequestDispatcher requestDispatcher;

    @SneakyThrows
    public void addState(String state, String key, String value) {
        Map<String, String> stringStringMap = STATE_PARAM_CACHE.get(state, HashMap::new);
        stringStringMap.put(key, value);
    }

    public void addOdcParam(String state, HttpServletRequest request) {
        String odcBackUrl = request.getParameter(OdcConstants.ODC_BACK_URL_PARAM);
        if (WebRequestUtils.isRedirectUrlValid(request, odcBackUrl)) {
            addState(state, OdcConstants.ODC_BACK_URL_PARAM, OdcInfoService.addOdcParameter(request, odcBackUrl));
        }
        String testLoginId = request.getParameter(OdcConstants.TEST_LOGIN_ID_PARAM);
        if (com.oceanbase.odc.common.util.StringUtils.isNotBlank(testLoginId)) {
            addState(state, OdcConstants.TEST_LOGIN_ID_PARAM, testLoginId);
        }
        String testLoginType = request.getParameter(OdcConstants.TEST_LOGIN_TYPE);
        if (com.oceanbase.odc.common.util.StringUtils.isNotBlank(testLoginType)) {
            addState(state, OdcConstants.TEST_LOGIN_TYPE, testLoginType);
        }
    }


    @SneakyThrows
    public Map<String, String> getStateParameter(String state) {
        return STATE_PARAM_CACHE.get(state, HashMap::new);
    };

    @SneakyThrows
    public void addStateToCurrentRequestParam() {
        HttpServletRequest request = WebRequestUtils.getCurrentRequest();
        Verify.notNull(request, "request");
        SuccessResponse<Map<String, String>> stateResponse = requestDispatcher
                .forward(requestDispatcher.getHostUrl(request.getServerName(), request.getServerPort()), HttpMethod.GET,
                        "/api/v2/sso/state?state=" + request.getParameter(OAuth2ParameterNames.STATE),
                        requestDispatcher.getRequestHeaders(request), null)
                .getContentByType(
                        new TypeReference<SuccessResponse<Map<String, String>>>() {});
        stateResponse.getData().forEach(request::setAttribute);
    }

}
