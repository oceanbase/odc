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
package com.oceanbase.odc.service.iam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
@RefreshScope
public class JwtProperties {
    @Value("${odc.iam.auth.jwt.expiration-seconds:900}")
    private long expireTimeSeconds;
    @Value("${odc.iam.auth.jwt.buffer-seconds:180}")
    private long bufferTimeSeconds;
    @Value("${odc.iam.auth.jwt.secret-key:#{null}}")
    private String tokenSecret;

    public long getExpireTimeMills() {
        return expireTimeSeconds * 1000;
    }

    public long getBufferTimeMills() {
        return bufferTimeSeconds * 1000;
    }

}
