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
package com.oceanbase.odc.service.ai.chat;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import com.oceanbase.odc.service.ai.chat.model.ChatProperties;

@Component
public class SqlCopilotRateLimiter {

    private final Cache<String, RateLimiter> userLimiters = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build();
    @Autowired
    private ChatProperties chatProperties;

    public boolean tryAcquire(String userId) {
        RateLimiter rateLimiter = userLimiters.get(userId,
                id -> RateLimiter.create(chatProperties.getMaxQpsPerUser()));
        return rateLimiter.tryAcquire();
    }
}
