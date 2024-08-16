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
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendResult;
import com.oceanbase.odc.service.notification.model.WebhookChannelConfig;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/24
 */
@Component("Feishu")
public class FeishuSender extends HttpSender {

    @Override
    public ChannelType type() {
        return ChannelType.Feishu;
    }

    @Override
    protected HttpHeaders getHeaders(Message message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Override
    protected String getBody(Message message) {
        Map<String, Object> params = new HashMap<>();
        params.put("msg_type", "text");
        params.put("content", ImmutableMap.of("text", message.getContent()));
        WebhookChannelConfig channelConfig = (WebhookChannelConfig) message.getChannel().getChannelConfig();
        try {
            if (StringUtils.isNotEmpty(channelConfig.getSign())) {
                long timestamp = System.currentTimeMillis() / 1000L;
                params.put("timestamp", timestamp + "");
                params.put("sign", sign(timestamp, channelConfig.getSign()));
            }
        } catch (Exception e) {
            throw new UnexpectedException("failed to calculate sign secret, reason: " + e.getMessage());
        }
        return JsonUtils.toJsonIgnoreNull(params);
    }

    @Override
    protected MessageSendResult checkResponse(Message message, ResponseEntity<String> response) {
        Map<String, Object> body = JsonUtils.fromJsonMap(response.getBody(), String.class, Object.class);
        if (Objects.isNull(body)) {
            return MessageSendResult.ofFail("empty response from Feishu");
        }
        if (Objects.equals("success", body.get("msg"))) {
            return MessageSendResult.ofSuccess();
        }
        return MessageSendResult.ofFail(String.format("Code: %s, message:%s", body.get("code"), body.get("msg")));
    }

    private String sign(Long timestamp, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        String stringToSign = timestamp + "\n" + secret;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(new byte[] {});
        return new String(Base64.encodeBase64(signData));
    }

}
