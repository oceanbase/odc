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
package com.oceanbase.odc.service.notification;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableMap;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.notification.helper.MessageResponseValidator;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendResult;
import com.oceanbase.odc.service.notification.model.WebhookChannelConfig;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/22
 */
@Component("Webhook")
public class HttpSender implements MessageSender {

    @Override
    public ChannelType type() {
        return ChannelType.Webhook;
    }

    @Override
    public MessageSendResult send(Message message) throws Exception {
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        RestTemplate restTemplate = new RestTemplate();
        setProxyIfNeed(restTemplate, channelConfig);
        HttpMethod httpMethod = channelConfig.getHttpMethod() == null ? HttpMethod.POST : channelConfig.getHttpMethod();
        HttpEntity<String> request = new HttpEntity<>(getBody(message), getHeaders(message));
        ResponseEntity<Map> response =
                restTemplate.exchange(getUrl(message), httpMethod, request, Map.class);
        return checkResponse(message, response);
    }

    protected String getUrl(Message message) {
        return ((WebhookChannelConfig) message.getChannel().getChannelConfig()).getWebhook();
    }

    protected HttpHeaders getHeaders(Message message) {
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        HttpHeaders headers = new HttpHeaders();
        String headersStr = channelConfig.getHeadersTemplate();
        if (StringUtils.isEmpty(headersStr)) {
            return headers;
        }
        String[] split = headersStr.split(";");
        for (String header : split) {
            String[] headerNameAndValue = header.split(":", 2);
            headers.add(headerNameAndValue[0].trim(), headerNameAndValue[1].trim());
        }
        return headers;
    }

    protected String getBody(Message message) {
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        StringSubstitutor substitutor = new StringSubstitutor(ImmutableMap.of("message", message.getContent()));
        String bodyTemplate = channelConfig.getBodyTemplate();
        return substitutor.replace(bodyTemplate);
    }

    protected MessageSendResult checkResponse(Message message, ResponseEntity<Map> response) {
        if (response.getStatusCode() != HttpStatus.OK) {
            String errorMessage =
                    response.getBody() == null ? "HttpCode:" + response.getStatusCode() : response.getBody().toString();
            return MessageSendResult.ofFail(errorMessage);
        }
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        String responseValidation = channelConfig.getResponseValidation();
        String content = MapUtils.isEmpty(response.getBody()) ? "" : JsonUtils.toJson(response.getBody());
        if (StringUtils.isEmpty(responseValidation) || "{}".equals(responseValidation)
                || MessageResponseValidator.validateMessage(content, responseValidation)) {
            return MessageSendResult.ofSuccess();
        }
        return MessageSendResult
                .ofFail(String.format("notify result is not expected, the expected result: %s, actual response: %s",
                        responseValidation, content));
    }

    private void setProxyIfNeed(RestTemplate restTemplate, WebhookChannelConfig channelConfig) {
        if (StringUtils.isEmpty(channelConfig.getHttpProxy())) {
            return;
        }
        String httpProxy = channelConfig.getHttpProxy();
        String[] proxyParas = httpProxy.split(":");
        if (proxyParas.length != 2) {
            throw new IllegalArgumentException("Illegal http proxy: " + httpProxy);
        }
        String hostName = proxyParas[0].replaceFirst("//", "");
        int port = Integer.parseInt(proxyParas[1]);
        Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(hostName, port));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(proxy);
        restTemplate.setRequestFactory(requestFactory);
    }

}
