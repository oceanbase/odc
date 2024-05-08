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
package com.oceanbase.odc.service.notification.helper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SSRFChecker;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.integration.HttpOperationService.IntegrationConfigProperties;
import com.oceanbase.odc.service.notification.NotificationProperties;
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

    @Autowired
    private IntegrationConfigProperties integrationConfigProperties;
    @Autowired
    private NotificationProperties notificationProperties;

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
        Verify.verify(
                channelConfig.getWebhook().startsWith("http://") || channelConfig.getWebhook().startsWith("https://"),
                "Webhook should start with 'http://' or 'https://'");
        try {
            if (CollectionUtils.isNotEmpty(integrationConfigProperties.getUrlWhiteList())) {
                Verify.verify(SSRFChecker.checkUrlInWhiteList(channelConfig.getWebhook(),
                        integrationConfigProperties.getUrlWhiteList()),
                        "The webhook is forbidden due to SSRF protection");
            } else {
                Verify.verify(SSRFChecker.checkHostNotInBlackList(new URL(channelConfig.getWebhook()).getHost(),
                        notificationProperties.getHostBlackList()),
                        "The webhook is forbidden due to SSRF protection");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String httpProxy = channelConfig.getHttpProxy();
        Verify.verify(StringUtils.isEmpty(httpProxy) || httpProxy.split(":").length == 3,
                "Illegal http proxy, it should be like 'http(s)://host:port'");

        String headersTemplate = channelConfig.getHeadersTemplate();
        if (StringUtils.isNotEmpty(headersTemplate)) {
            String[] split = headersTemplate.split(";");
            for (String header : split) {
                Verify.verify(2 == header.split(":").length, "Invalid header: " + header);
            }
        }

        String responseValidation = channelConfig.getResponseValidation();
        if (StringUtils.isNotEmpty(responseValidation) && responseValidation.startsWith("{")) {
            Map map = JsonUtils.fromJson(responseValidation, Map.class);
            if (MapUtils.isEmpty(map)) {
                throw new IllegalArgumentException("Please enter a valid Json map for validation");
            }
        }

    }
}
