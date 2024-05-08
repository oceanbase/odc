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

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/3/23 10:20
 * @Description: []
 */
@Data
@Configuration
@RefreshScope
@ConfigurationProperties("odc.notification")
public class NotificationProperties {
    private boolean enabled;

    private int eventDequeueBatchSize;

    private int notificationDequeueBatchSize;

    private int maxResendTimes;

    private int dequeueEventFixedDelayMillis;

    private int dequeueCreatedNotificationFixedDelayMillis;

    private int dequeueFailedNotificationFixedDelayMillis;

    private int dequeueSendingNotificationFixedDelayMillis;

    private List<String> hostBlackList;

}
