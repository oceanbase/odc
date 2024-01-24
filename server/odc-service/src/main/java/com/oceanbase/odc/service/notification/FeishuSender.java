/*
 * Copyright (c) 2024 OceanBase.
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

import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Message;
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
    protected String getBody(Message message) {
        Map<String, String> params = new HashMap<>();
        params.put("msg_type", "text");
        params.put("content", message.getContent());
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

}
