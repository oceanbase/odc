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
package com.oceanbase.odc.server.web.highavailable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "odc.web.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String[] urlWhiteList = new String[] {"/api/v1/info", "/sqls/getResult"};
    private BucketProperties api = new BucketProperties(20, 10, 5);
    private BucketProperties sql = new BucketProperties(10000L, 1000L, 10L);

    @Data
    @NoArgsConstructor
    public static class BucketProperties {
        private long capacity = 10;
        private long refillTokens = 5;
        private long refillDurationSeconds = 1;

        public BucketProperties(long capacity, long refillTokens, long refillDurationSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillDurationSeconds = refillDurationSeconds;
        }
    }
}
