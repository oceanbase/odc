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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendResult;
import com.oceanbase.odc.service.notification.model.WeComChannelConfig;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/24
 */
@Component("WeCom")
public class WeComSender extends HttpSender {

    @Override
    public ChannelType type() {
        return ChannelType.WeCom;
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
        // WeChat does not support @ user when using markdown
        params.put("msgtype", "text");
        Map<String, Object> text = new HashMap<>();
        text.put("content", message.getContent());
        WeComChannelConfig channelConfig = (WeComChannelConfig) message.getChannel().getChannelConfig();
        text.put("mentioned_mobile_list", channelConfig.getAtMobiles());
        params.put("text", text);
        return JsonUtils.toJsonIgnoreNull(params);
    }

    @Override
    protected MessageSendResult checkResponse(Message message, ResponseEntity<String> response) {
        Map<String, Object> body = JsonUtils.fromJsonMap(response.getBody(), String.class, Object.class);
        if (Objects.isNull(body)) {
            return MessageSendResult.ofFail("empty response from WeCom");
        }
        if (Objects.equals(0, body.get("errcode"))) {
            return MessageSendResult.ofSuccess();
        }
        return MessageSendResult.ofFail(String.format("Code: %s, message:%s", body.get("errcode"), body.get("errmsg")));
    }

}
