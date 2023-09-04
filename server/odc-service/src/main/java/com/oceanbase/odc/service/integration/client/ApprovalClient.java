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

import javax.annotation.PostConstruct;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.ExternalServiceError;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.integration.HttpOperationService;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.ApprovalProperties.StartProperties;
import com.oceanbase.odc.service.integration.model.ApprovalProperties.StatusProperties;
import com.oceanbase.odc.service.integration.model.ApprovalStatus;
import com.oceanbase.odc.service.integration.model.Encryption;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.ApiProperties;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.HttpProperties;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;
import com.oceanbase.odc.service.integration.util.EncryptionUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/3/27 19:43
 */
@Slf4j
@Component
public class ApprovalClient {
    @Autowired
    private HttpOperationService httpService;

    private HttpClient httpClient;

    @Value("${odc.integration.approval.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${odc.integration.approval.socket-timeout-seconds:30}")
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
        log.debug("Approval integration HTTP client initialized, requestConfig={}", requestConfig);
    }

    /**
     * Start a external approval process instance
     * 
     * @param properties Properties for invoking external API, {@link ApprovalProperties}
     * @param variables Template variables for building request, more details reference {@link Variable}
     * @return The process instance ID
     */
    public String start(@NonNull ApprovalProperties properties, TemplateVariables variables) {
        StartProperties start = properties.getApi().getStart();
        HttpProperties http = properties.getHttp();
        Encryption encryption = properties.getEncryption();
        HttpUriRequest request;
        try {
            request = httpService.buildHttpRequest(start, http, encryption, variables);
        } catch (Exception e) {
            throw new UnexpectedException("Build request failed: " + e.getMessage());
        }
        String response;
        try {
            response = httpClient.execute(request, new BasicResponseHandler());
        } catch (Exception e) {
            throw new ExternalServiceError(ErrorCodes.ExternalServiceError,
                    "Request execute failed: " + e.getMessage());
        }
        String decrypt = EncryptionUtil.decrypt(response, encryption);
        checkResponse(decrypt, start.getRequestSuccessExpression());
        try {
            return httpService.extractHttpResponse(decrypt, start.getExtractInstanceIdExpression(), String.class);
        } catch (Exception e) {
            throw new UnexpectedException("Extract process instance ID failed: " + e.getMessage());
        }
    }

    /**
     * Query the status of external process instance
     *
     * @param properties Properties for invoking external API, {@link ApprovalProperties}
     * @param variables Template variables for building request, more details reference {@link Variable}
     * @return The status of approval instance, {@link ApprovalStatus}
     */
    public ApprovalStatus status(@NonNull ApprovalProperties properties, TemplateVariables variables) {
        StatusProperties status = properties.getApi().getStatus();
        HttpProperties http = properties.getHttp();
        Encryption encryption = properties.getEncryption();
        HttpUriRequest request;
        try {
            request = httpService.buildHttpRequest(status, http, encryption, variables);
        } catch (Exception e) {
            throw new UnexpectedException("Build request failed: " + e.getMessage());
        }
        String response;
        try {
            response = httpClient.execute(request, new BasicResponseHandler());
        } catch (Exception e) {
            throw new ExternalServiceError(ErrorCodes.ExternalServiceError,
                    "Request execute failed: " + e.getMessage());
        }
        String decrypt = EncryptionUtil.decrypt(response, encryption);
        checkResponse(decrypt, status.getRequestSuccessExpression());
        try {
            if (httpService.extractHttpResponse(decrypt, status.getProcessTerminatedExpression(), Boolean.class)) {
                return ApprovalStatus.TERMINATED;
            } else if (httpService.extractHttpResponse(decrypt, status.getProcessPendingExpression(), Boolean.class)) {
                return ApprovalStatus.PENDING;
            } else if (httpService.extractHttpResponse(decrypt, status.getProcessApprovedExpression(), Boolean.class)) {
                return ApprovalStatus.APPROVED;
            } else if (httpService.extractHttpResponse(decrypt, status.getProcessRejectedExpression(), Boolean.class)) {
                return ApprovalStatus.REJECTED;
            } else {
                throw new RuntimeException("Response mismatch any status expression, response body: " + decrypt);
            }
        } catch (Exception e) {
            throw new UnexpectedException("Extract process instance status failed: " + e.getMessage());
        }
    }

    /**
     * Cancel a exists process instance
     *
     * @param properties Properties for invoking external API, {@link ApprovalProperties}
     * @param variables Template variables for building request, more details reference {@link Variable}
     */
    public void cancel(@NonNull ApprovalProperties properties, TemplateVariables variables) {
        ApiProperties cancel = properties.getApi().getCancel();
        HttpProperties http = properties.getHttp();
        Encryption encryption = properties.getEncryption();
        HttpUriRequest request;
        try {
            request = httpService.buildHttpRequest(cancel, http, encryption, variables);
        } catch (Exception e) {
            throw new UnexpectedException("Build request failed: " + e.getMessage());
        }
        String response;
        try {
            response = httpClient.execute(request, new BasicResponseHandler());
        } catch (Exception e) {
            throw new ExternalServiceError(ErrorCodes.ExternalServiceError,
                    "Request execute failed: " + e.getMessage());
        }
        String decrypt = EncryptionUtil.decrypt(response, encryption);
        checkResponse(decrypt, cancel.getRequestSuccessExpression());
    }

    /**
     * Build hyperlink for accessing integrated approval system or platform
     * 
     * @param expression Expression for building customized hyperlink
     * @param variables Template variables for building request, more details reference {@link Variable}
     * @return URL hyperlink
     */
    public String buildHyperlink(@NonNull String expression, TemplateVariables variables) {
        return variables.process(expression);
    }

    private void checkResponse(String response, String expression) {
        boolean valid;
        try {
            valid = httpService.extractHttpResponse(response, expression, Boolean.class);
        } catch (Exception e) {
            throw new UnexpectedException("Extract request success expression failed: " + e.getMessage());
        }
        Verify.verify(valid, "Response is invalid, except: " + expression + ", response body: " + response);
    }

}
