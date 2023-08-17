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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AnttechOceanbasePassinfoLogininfoQueryRequest;
import com.alipay.api.response.AnttechOceanbasePassinfoLogininfoQueryResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PlaysiteOpenAPIClient {

    @Autowired
    private PlaysiteOpenApiProperties alipayOpenApiProperties;

    public PlaysiteUser acquireObUserInfo(Object token) throws AlipayApiException {
        String tokenStr = (String) token;
        AlipayClient client =
                new DefaultAlipayClient(alipayOpenApiProperties.getServerUrl(), alipayOpenApiProperties.getAppId(),
                        alipayOpenApiProperties.getPrivateKey(), alipayOpenApiProperties.getFormat(),
                        alipayOpenApiProperties.getCharset(),
                        alipayOpenApiProperties.getAlipayPublicKey(),
                        alipayOpenApiProperties.getSignType());
        AnttechOceanbasePassinfoLogininfoQueryRequest userInfoRequest =
                new AnttechOceanbasePassinfoLogininfoQueryRequest();
        userInfoRequest.setBizContent(String.format("{" +
                "\"ob_auth_token\":\"%s\"," +
                "\"role_type\":\"MEMBER\"," +
                "\"renew\":true" +
                "  }", tokenStr));
        AnttechOceanbasePassinfoLogininfoQueryResponse response = client.execute(userInfoRequest);
        PlaysiteUser obOfficialWebsiteUser;
        if (response.isSuccess()) {
            obOfficialWebsiteUser =
                    new PlaysiteUser(response.getRoleType(), response.getPassportId(), response.getEntityId());
            log.info("Acquire Ob official website user info successfully");
        } else {
            log.warn("Acquire Ob official website user info failed, may due to invalid obAuthToken");
            throw new AccessDeniedException(ErrorCodes.LoginExpired, "token is invalid");
        }
        return obOfficialWebsiteUser;
    }

}
