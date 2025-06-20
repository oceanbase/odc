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
package com.oceanbase.odc.service.integration.client;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.MapUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.ExternalServiceError;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.integration.HttpOperationService;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.Encryption;
import com.oceanbase.odc.service.integration.model.IntegrationProperties;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.ApiProperties;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.HttpProperties;
import com.oceanbase.odc.service.integration.model.OdcIntegrationResponse;
import com.oceanbase.odc.service.integration.model.SqlCheckResult;
import com.oceanbase.odc.service.integration.model.SqlCheckStatus;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties.CallBackProperties;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties.CheckProperties;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;
import com.oceanbase.odc.service.integration.util.EncryptionUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/3/30 20:04
 */
@Slf4j
@Component
public class SqlInterceptorClient {
    @Autowired
    private HttpOperationService httpService;

    private HttpClient httpClient;

    @Value("${odc.integration.sql-interceptor.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${odc.integration.sql-interceptor.socket-timeout-seconds:30}")
    private int socketTimeoutSeconds;

    @PostConstruct
    public void init() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectTimeoutSeconds * 1000)
                .setConnectTimeout(connectTimeoutSeconds * 1000)
                .setSocketTimeout(socketTimeoutSeconds * 1000)
                .build();
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
        log.debug("SQL interceptor integration HTTP client initialized, requestConfig={}", requestConfig);
    }

    /**
     * Check the status of SQL content
     *
     * @param properties Properties for invoking external API, {@link ApprovalProperties}
     * @param variables Template variables for building request, more details reference {@link Variable}
     * @return The check result {@link SqlCheckStatus} of SQL content
     */
    public SqlCheckResult check(@NonNull SqlInterceptorProperties properties, TemplateVariables variables) {
        CheckProperties check = properties.getApi().getCheck();
        HttpProperties http = properties.getHttp();
        Encryption encryption = properties.getEncryption();
        OdcIntegrationResponse response = getIntegrationResponse(http, check, variables, encryption);
        try {
            SqlCheckStatus sqlCheckStatus = null;
            String expression = check.getRequestSuccessExpression();
            boolean valid = httpService.extractHttpResponse(response, expression, Boolean.class);
            Verify.verify(valid, "Response is invalid, except: " + expression + ", response body: " + response);
            if (httpService.extractHttpResponse(response, check.getInWhiteListExpression(), Boolean.class)) {
                sqlCheckStatus = SqlCheckStatus.IN_WHITE_LIST;
            } else if (httpService.extractHttpResponse(response, check.getInBlackListExpression(), Boolean.class)) {
                sqlCheckStatus = SqlCheckStatus.IN_BLACK_LIST;
            } else if (httpService.extractHttpResponse(response, check.getNeedReviewExpression(), Boolean.class)) {
                sqlCheckStatus = SqlCheckStatus.NEED_REVIEW;
            } else {
                throw new RuntimeException(
                        "Response mismatch any check result expression, response body: " + response.getContent());
            }
            // try extract value from response for future request
            Map<String, String> extractedResponse = new HashMap<>();
            CallBackProperties callBackProperties = check.getCallback();
            if (null != callBackProperties && !MapUtils.isEmpty(callBackProperties.getResponseExtractExpressions())) {
                for (Map.Entry<String, String> responseExtractExpressionEntrySet : callBackProperties
                        .getResponseExtractExpressions().entrySet()) {
                    String key = responseExtractExpressionEntrySet.getKey();
                    String responseExtractExpression = responseExtractExpressionEntrySet.getValue();
                    String value = extractedResponse.put(key,
                            httpService.extractHttpResponse(response, responseExtractExpression, String.class));
                    if (null != value) {
                        extractedResponse.put(key, value);
                    }
                }
            }
            return new SqlCheckResult(extractedResponse, sqlCheckStatus);
        } catch (Exception e) {
            throw new UnexpectedException("Extract SQL check result failed: " + e.getMessage());
        }
    }

    public OdcIntegrationResponse getIntegrationResponse(HttpProperties http,
            @NonNull IntegrationProperties.ApiProperties api, TemplateVariables variables, Encryption encryption) {
        HttpUriRequest request;
        try {
            request = httpService.buildHttpRequest(api, http, encryption, variables);
        } catch (Exception e) {
            throw new UnexpectedException("Build request failed: " + e.getMessage());
        }
        OdcIntegrationResponse response;
        try {
            response = httpClient.execute(request, new OdcIntegrationResponseHandler());
        } catch (Exception e) {
            throw new ExternalServiceError(ErrorCodes.ExternalServiceError,
                    "Request execute failed: " + e.getMessage());
        }
        response.setContent(EncryptionUtil.decrypt(response.getContent(), encryption));
        log.info("sqlInterceptorClient getIntegrationResponse request = {}, response ={}", request,
                response.getContent());
        return response;
    }
}
