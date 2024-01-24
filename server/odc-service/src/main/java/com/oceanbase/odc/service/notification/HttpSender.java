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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
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
        HttpEntity<String> request = new HttpEntity<>(getBody(message), getHeaders(message));
        ResponseEntity<Object> response =
                new RestTemplate().exchange(getUrl(message), HttpMethod.POST, request, Object.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            String errorMessage =
                    response.getBody() == null ? "HttpCode:" + response.getStatusCode() : response.getBody().toString();
            return MessageSendResult.ofFail(errorMessage);
        }
        return MessageSendResult.ofSuccess();
    }

    protected String getUrl(Message message) {
        return ((WebhookChannelConfig) message.getChannel().getChannelConfig()).getWebhook();
    }

    protected HttpHeaders getHeaders(Message message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected String getBody(Message message) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", message.getTitle());
        params.put("content", message.getContent());
        params.put("project", message.getProjectId());
        long timestamp = System.currentTimeMillis();
        params.put("timestamp", timestamp);
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        try {
            if (StringUtils.isNotEmpty(channelConfig.getSign())) {
                params.put("sign", sign(timestamp, channelConfig.getSign()));
            }
        } catch (Exception e) {
            throw new UnexpectedException("failed to calculate sign secret, reason: " + e.getMessage());
        }
        return JsonUtils.toJsonIgnoreNull(params);
    }

    protected String sign(Long timestamp, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        String stringToSign = timestamp + "\n" + secret;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(new byte[] {});
        return new String(Base64.encodeBase64(signData));
    }

}
