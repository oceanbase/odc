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
package com.oceanbase.odc.service.bastion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.common.json.JsonPathUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.bastion.model.BastionAccount;
import com.oceanbase.odc.service.bastion.model.BastionProperties;
import com.oceanbase.odc.service.bastion.model.BastionProperties.HttpProperties;
import com.oceanbase.odc.service.bastion.model.BastionProperties.QueryProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
class BastionAccountClient {
    private static final String VARIABLE_NAME_TOKEN = "account_verify_token";
    private static final SpelParserConfiguration SPEL_CONFIG = new SpelParserConfiguration(true, true);
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser(SPEL_CONFIG);

    @Autowired
    private BastionProperties bastionProperties;

    @Autowired
    private BastionEncryptionService bastionEncryptionService;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        HttpProperties httpProperties = httpProperties();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(httpProperties.getConnectTimeoutSeconds() * 1000)
                .setConnectTimeout(httpProperties.getConnectTimeoutSeconds() * 1000)
                .setSocketTimeout(httpProperties.getReadTimeoutSeconds() * 1000)
                .build();
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
        log.debug("BastionAccountClient initialized, httpProperties={},queryProperties={}",
                httpProperties, queryProperties());
    }

    BastionAccount query(String token) {
        log.debug("Start query account, token={}", token);
        if (StringUtils.isBlank(token)) {
            throw new PreAuthenticatedCredentialsNotFoundException("token not found for bastion account query");
        }
        HttpUriRequest request;
        try {
            request = buildRequest(token);
        } catch (Exception ex) {
            throw new AuthenticationServiceException("Build request failed: " + ex.getMessage());
        }
        String responseBody;
        try {
            responseBody = httpClient.execute(request, new BasicResponseHandler());
        } catch (Exception ex) {
            throw new AuthenticationServiceException("Send HTTP request failed: " + ex.getMessage());
        }
        try {
            return extractResponse(responseBody);
        } catch (Exception ex) {
            throw new AuthenticationServiceException("Extract response failed: " + ex.getMessage());
        }
    }

    @VisibleForTesting
    HttpUriRequest buildRequest(String token) {
        HttpProperties httpProperties = httpProperties();
        QueryProperties queryProperties = queryProperties();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(httpProperties.getConnectTimeoutSeconds() * 1000)
                .setConnectTimeout(httpProperties.getConnectTimeoutSeconds() * 1000)
                .setSocketTimeout(httpProperties.getReadTimeoutSeconds() * 1000).build();

        Map<String, String> variables = buildVariables(token);
        Template template = new Template(variables);

        // init request, url/headers/body support template variables
        RequestBuilder requestBuilder = RequestBuilder.create(queryProperties.getRequestMethod())
                .setConfig(requestConfig);

        // set request url
        String processedRequestUrl = template.process(queryProperties.getRequestUrl());
        requestBuilder.setUri(processedRequestUrl);

        // set request headers
        List<String> requestHeaders = queryProperties.getRequestHeaders();
        if (CollectionUtils.isNotEmpty(requestHeaders)) {
            for (String header : requestHeaders) {
                String[] split = header.split("=");
                Verify.verify(split.length > 1, "Invalid header format, header=" + header);
                String name = split[0];
                String value = split[1];
                Verify.notBlank(name, "header.name");
                Verify.notBlank(value, "header.value");
                String processedHeaderValue = template.process(value);
                requestBuilder.addHeader(name, processedHeaderValue);
            }
        }

        // set request body
        String requestBodyTemplate = queryProperties.getRequestBody();
        if (StringUtils.isNotEmpty(requestBodyTemplate)) {
            String plainRequestBody = template.process(requestBodyTemplate);
            String encryptedRequestBody = encryptRequest(plainRequestBody);
            log.debug("Build request, plainRequestBody={}, encryptedRequestBody={}",
                    plainRequestBody, encryptedRequestBody);
            HttpEntity httpEntity = EntityBuilder.create()
                    .setText(encryptedRequestBody)
                    .build();
            requestBuilder.setEntity(httpEntity);
        }
        return requestBuilder.build();
    }

    @VisibleForTesting
    BastionAccount extractResponse(String responseBody) {
        QueryProperties query = bastionProperties.getAccount().getQuery();

        String plainResponseBody = decryptResponse(responseBody);

        log.debug("Extract response, responseBody={}, plainResponseBody={}",
                responseBody, plainResponseBody);

        Object responseObject = JsonPathUtils.read(plainResponseBody, "$");

        String responseBodyValidExpression = query.getResponseBodyValidExpression();
        if (StringUtils.isNotBlank(responseBodyValidExpression)) {
            Expression responseBodyValid = SPEL_PARSER.parseExpression(responseBodyValidExpression);
            Boolean value = responseBodyValid.getValue(responseObject, Boolean.class);
            Verify.verify(Boolean.TRUE.equals(value),
                    "Response body invalid, expect " + responseBodyValidExpression
                            + " , response body " + plainResponseBody);
        }

        String responseBodyUsernameExtractExpression = query.getResponseBodyUsernameExtractExpression();
        Expression responseBodyUsernameExtract = SPEL_PARSER.parseExpression(responseBodyUsernameExtractExpression);

        String username = responseBodyUsernameExtract.getValue(responseObject, String.class);
        Verify.notBlank(username, "Extract username got blank result, expression="
                + responseBodyUsernameExtractExpression + ", response body " + plainResponseBody);

        BastionAccount account = new BastionAccount();
        account.setUsername(username);

        String responseBodyNickNameExtractExpression = query.getResponseBodyNickNameExtractExpression();
        String nickName = null;
        if (StringUtils.isNotBlank(responseBodyNickNameExtractExpression)) {
            Expression responseBodyNickNameExtract = SPEL_PARSER.parseExpression(responseBodyNickNameExtractExpression);
            nickName = responseBodyNickNameExtract.getValue(responseObject, String.class);
        }
        account.setNickName(StringUtils.isBlank(nickName) ? username : nickName);
        return account;
    }

    private String encryptRequest(String plainText) {
        return Boolean.TRUE.equals(queryProperties().getRequestEncrypted())
                ? bastionEncryptionService.encrypt(plainText)
                : plainText;
    }

    private String decryptResponse(String encryptedText) {
        return Boolean.TRUE.equals(queryProperties().getResponseEncrypted())
                ? bastionEncryptionService.decrypt(encryptedText)
                : encryptedText;
    }

    private HttpProperties httpProperties() {
        return bastionProperties.getAccount().getHttp();
    }

    private QueryProperties queryProperties() {
        return bastionProperties.getAccount().getQuery();
    }

    private Map<String, String> buildVariables(String token) {
        Map<String, String> variables = new HashMap<>();
        variables.put(VARIABLE_NAME_TOKEN, token);
        return variables;
    }

    /**
     * 处理模板变量
     */
    private static class Template {
        private final Map<String, String> variables;

        private Template(Map<String, String> variables) {
            this.variables = variables;
        }

        private String process(String source) {
            StringSubstitutor sub = new StringSubstitutor(this.variables).setDisableSubstitutionInValues(true);
            return sub.replace(source);
        }
    }

}
