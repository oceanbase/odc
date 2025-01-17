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
import java.util.regex.Pattern;

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
    private static final Pattern ILLEGAL_CHARACTERS_PATTERN = Pattern.compile("[@#;$,\\[\\]{}\\\\^\"<>]");

    @Autowired
    private IntegrationConfigProperties integrationConfigProperties;
    @Autowired
    private NotificationProperties notificationProperties;

    public void validate(@NonNull ChannelType type, BaseChannelConfig channelConfig) {
        switch (type) {
            case DingTalk:
                validateWebhook(((DingTalkChannelConfig) channelConfig).getWebhook());
                return;
            case WeCom:
                validateWebhook(((WeComChannelConfig) channelConfig).getWebhook());
                return;
            case Feishu:
                validateWebhook(((WebhookChannelConfig) channelConfig).getWebhook());
                return;
            case Webhook:
                validateWebhookChannelConfig((WebhookChannelConfig) channelConfig);
                return;
            default:
                throw new NotImplementedException();
        }
    }

    private void validateWebhookChannelConfig(WebhookChannelConfig channelConfig) {
        validateWebhook(channelConfig.getWebhook());

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

    private void validateWebhook(String webhook) {
        Verify.notEmpty(webhook, "webhook");
        Verify.verify(
                webhook.startsWith("http://") || webhook.startsWith("https://"),
                "Webhook should start with 'http://' or 'https://'");
        if (ILLEGAL_CHARACTERS_PATTERN.matcher(webhook).find()) {
            throw new IllegalArgumentException("Webhook contains illegal characters");
        }
        try {
            if (CollectionUtils.isNotEmpty(integrationConfigProperties.getUrlWhiteList())) {
                Verify.verify(SSRFChecker.checkUrlInWhiteList(webhook,
                        integrationConfigProperties.getUrlWhiteList()),
                        "The webhook is forbidden due to SSRF protection");
            } else {
                Verify.verify(SSRFChecker.checkHostNotInBlackList(new URL(webhook).getHost(),
                        notificationProperties.getHostBlackList()),
                        "The webhook is forbidden due to SSRF protection");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
