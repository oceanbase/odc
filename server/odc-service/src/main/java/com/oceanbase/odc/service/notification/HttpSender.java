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
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.oceanbase.odc.service.notification.helper.MessageResponseValidator;
import com.oceanbase.odc.service.notification.helper.MessageTemplateProcessor;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendResult;
import com.oceanbase.odc.service.notification.model.WebhookChannelConfig;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/22
 */
@Component("Webhook")
public class HttpSender implements MessageSender {

    @Autowired
    private NotificationProperties notificationProperties;

    @Override
    public ChannelType type() {
        return ChannelType.Webhook;
    }

    @Override
    public MessageSendResult send(Message message) throws Exception {
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        RestTemplate restTemplate = getRestTemplate(channelConfig);
        HttpMethod httpMethod = channelConfig.getHttpMethod() == null ? HttpMethod.POST : channelConfig.getHttpMethod();
        HttpEntity<String> request = new HttpEntity<>(getBody(message), getHeaders(message));
        ResponseEntity<String> response =
                restTemplate.exchange(getUrl(message), httpMethod, request, String.class);
        return checkResponse(message, response);
    }

    protected String getUrl(Message message) {
        return ((WebhookChannelConfig) message.getChannel().getChannelConfig()).getWebhook();
    }

    protected HttpHeaders getHeaders(Message message) {
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        HttpHeaders headers = new HttpHeaders();
        String headersTemplate = channelConfig.getHeadersTemplate();
        if (StringUtils.isEmpty(headersTemplate)) {
            return headers;
        }
        String[] split = resolveTemplate(message, headersTemplate).split(";");
        for (String header : split) {
            String[] headerNameAndValue = header.split(":", 2);
            if (headerNameAndValue.length != 2) {
                continue;
            }
            headers.add(headerNameAndValue[0].trim(), headerNameAndValue[1].trim());
        }
        return headers;
    }

    protected String getBody(Message message) {
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        return resolveTemplate(message, channelConfig.getBodyTemplate());
    }

    protected MessageSendResult checkResponse(Message message, ResponseEntity<String> response) {
        if (response.getStatusCode() != HttpStatus.OK) {
            String errorMessage =
                    response.getBody() == null ? "HttpCode:" + response.getStatusCode() : response.getBody();
            return MessageSendResult.ofFail(errorMessage);
        }
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        String responseValidation = channelConfig.getResponseValidation();
        String content = StringUtils.isEmpty(response.getBody()) ? "" : response.getBody();
        if (StringUtils.isEmpty(responseValidation) || "{}".equals(responseValidation)
                || MessageResponseValidator.validateMessage(content, responseValidation)) {
            return MessageSendResult.ofSuccess();
        }
        return MessageSendResult
                .ofFail(String.format("notify result is not expected, the expected result: %s, actual response: %s",
                        responseValidation, content));
    }

    private RestTemplate getRestTemplate(WebhookChannelConfig channelConfig) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) notificationProperties.getSendTimeoutMillis());
        requestFactory.setReadTimeout((int) notificationProperties.getSendTimeoutMillis());
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        if (StringUtils.isEmpty(channelConfig.getHttpProxy())) {
            return restTemplate;
        }
        String httpProxy = channelConfig.getHttpProxy();
        String[] proxyParas = httpProxy.split(":");
        String hostName = proxyParas[1].replaceFirst("//", "");
        int port = Integer.parseInt(proxyParas[2]);
        Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(hostName, port));
        requestFactory.setProxy(proxy);
        return restTemplate;
    }

    private String resolveTemplate(Message message, String template) {
        Event event = message.getEvent();
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        Locale locale;
        try {
            locale = Locale.forLanguageTag(channelConfig.getLanguage());
        } catch (Exception e) {
            locale = Locale.getDefault();
        }
        EventLabels labels;
        if (event == null || event.getLabels() == null) {
            labels = new EventLabels();
        } else {
            labels = event.getLabels();
        }
        labels.putIfNonNull("message", message.getContent());
        labels.putIfNonNull("title", message.getTitle());
        return MessageTemplateProcessor.replaceVariables(template, locale, labels);
    }

}
