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
package com.oceanbase.odc.service.integration;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonPathUtils;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.integration.model.Encryption;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.ApiProperties;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.Body;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.BodyType;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.HttpProperties;
import com.oceanbase.odc.service.integration.model.OdcIntegrationResponse;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.util.EncryptionUtil;

import lombok.Data;
import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/4/11 15:03
 */
@Component
public class HttpOperationService {

    @Autowired
    private IntegrationConfigProperties configProperties;

    private static final SpelParserConfiguration SPEL_PARSER_CONFIGURATION = new SpelParserConfiguration(true, true);
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser(SPEL_PARSER_CONFIGURATION);

    @SuppressWarnings("unchecked")
    public HttpUriRequest buildHttpRequest(@NonNull ApiProperties api, @NonNull HttpProperties httpProperties,
            @NonNull Encryption encryption, TemplateVariables variables) throws UnsupportedEncodingException {
        // init request
        RequestBuilder builder = RequestBuilder.create(api.getMethod().name());
        // set request config
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(httpProperties.getConnectTimeoutSeconds() * 1000)
                .setConnectTimeout(httpProperties.getConnectTimeoutSeconds() * 1000)
                .setSocketTimeout(httpProperties.getSocketTimeoutSeconds() * 1000)
                .build();
        builder.setConfig(requestConfig);
        // set url
        PreConditions.validInUrlWhiteList(api.getUrl(), configProperties.getUrlWhiteList());
        builder.setUri(variables.process(api.getUrl()));
        // set headers
        if (Objects.nonNull(api.getHeaders())) {
            for (Entry<String, String> entry : api.getHeaders().entrySet()) {
                builder.addHeader(entry.getKey(), variables.process(entry.getValue()));
            }
        }
        // set params
        if (Objects.nonNull(api.getQueryParameters())) {
            for (Entry<String, String> entry : api.getQueryParameters().entrySet()) {
                builder.addParameter(entry.getKey(), variables.process(entry.getValue()));
            }
        }
        // set body
        Body body = api.getBody();
        if (Objects.nonNull(body)) {
            BodyType type = body.getType();
            if (type == BodyType.RAW) {
                String json = variables.process((String) body.getContent());
                String encryptedBody = EncryptionUtil.encrypt(json, encryption);
                HttpEntity entity = EntityBuilder.create().setText(encryptedBody).build();
                builder.setEntity(entity);
            } else if (type == BodyType.FORM_DATA) {
                Map<String, Object> formData = (Map<String, Object>) body.getContent();
                List<NameValuePair> paramList = new ArrayList<>();
                for (String key : formData.keySet()) {
                    paramList.add(new BasicNameValuePair(key, variables.process(formData.get(key).toString())));
                }
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(paramList, "utf-8");
                builder.setEntity(formEntity);
            } else {
                throw new UnexpectedException("Unexpected body type: " + type);
            }
        }
        return builder.build();
    }

    public <T> T extractHttpResponse(OdcIntegrationResponse decryptedResponse, String extractExpression,
            Class<T> type) {
        String content = decryptedResponse.getContent();
        String mineType = decryptedResponse.getContentType().getMimeType();
        // ODC treats the response body as a JSON string by default.
        // If the Content-Type is XML, then try to convert it to JSON.
        switch (mineType) {
            case "text/xml":
            case "application/xml":
            case "application/atom+xml":
            case "application/xhtml+xml":
            case "application/soap+xml":
                content = JsonUtils.xmlToJson(content);
                break;
            default:
                break;
        }
        Object responseObject = JsonPathUtils.read(content, "$");
        StandardEvaluationContext context = new StandardEvaluationContext(responseObject);
        // If you don't have MapAccessor configured, you can only use [a][b] to parse.
        context.addPropertyAccessor(new MapAccessor());
        Expression expression = EXPRESSION_PARSER.parseExpression(extractExpression);
        return expression.getValue(context, type);
    }

    @RefreshScope
    @Configuration
    @Data
    private static class IntegrationConfigProperties {

        @Value("${odc.integration.url-white-list:}")
        private List<String> urlWhiteList;

    }

}
