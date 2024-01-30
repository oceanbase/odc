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

package com.oceanbase.odc.service.notification.helper;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.notification.model.BaseChannelConfig;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.DingTalkChannelConfig;
import com.oceanbase.odc.service.notification.model.WeComChannelConfig;
import com.oceanbase.odc.service.notification.model.WebhookChannelConfig;

import lombok.NonNull;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/30
 */
@Component
public class ChannelConfigValidator {
    private static final String DINGTALK_WEBHOOK_PREFIX = "https://oapi.dingtalk.com/robot";
    private static final String FEISHU_WEBHOOK_PREFIX = "https://open.feishu.cn/open-apis/bot";
    private static final String WECOM_WEBHOOK_PREFIX = "https://qyapi.weixin.qq.com/cgi-bin/webhook";

    public void validate(@NonNull ChannelType type, BaseChannelConfig channelConfig) {
        switch (type) {
            case DingTalk:
                validateDingTalkChannelConfig((DingTalkChannelConfig) channelConfig);
                return;
            case WeCom:
                validateWeComChannelConfig((WeComChannelConfig) channelConfig);
                return;
            case Feishu:
                validateFeishuChannelConfig((WebhookChannelConfig) channelConfig);
                return;
            case Webhook:
                validateWebhookChannelConfig((WebhookChannelConfig) channelConfig);
                return;
            default:
                throw new NotImplementedException();
        }
    }

    private void validateDingTalkChannelConfig(DingTalkChannelConfig channelConfig) {
        Verify.notEmpty(channelConfig.getWebhook(), "webhook");
        Verify.verify(channelConfig.getWebhook().startsWith(DINGTALK_WEBHOOK_PREFIX),
                "please input an valid Dingtalk webhook");
    }

    private void validateFeishuChannelConfig(WebhookChannelConfig channelConfig) {
        Verify.notEmpty(channelConfig.getWebhook(), "webhook");
        Verify.verify(channelConfig.getWebhook().startsWith(FEISHU_WEBHOOK_PREFIX),
                "please input an valid Feishu webhook");
    }

    private void validateWeComChannelConfig(WeComChannelConfig channelConfig) {
        Verify.notEmpty(channelConfig.getWebhook(), "webhook");
        Verify.verify(channelConfig.getWebhook().startsWith(WECOM_WEBHOOK_PREFIX),
                "please input an valid WeCom webhook");
    }

    private void validateWebhookChannelConfig(WebhookChannelConfig channelConfig) {
        Verify.notEmpty(channelConfig.getWebhook(), "webhook");
    }

}
