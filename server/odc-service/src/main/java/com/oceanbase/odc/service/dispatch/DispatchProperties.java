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
package com.oceanbase.odc.service.dispatch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Properties for {@code RPC}
 *
 * @author yh263208
 * @date 2022-03-24 16:36
 * @since ODC_release_3.3.0
 */
@Data
@RefreshScope
@Configuration
public class DispatchProperties {
    /**
     * rpc connect timeout, default is 10s
     */
    @Value("${odc.rpc.connect-timeout-seconds:10}")
    private long connectTimeoutSeconds = 10;
    /**
     * rpc connect timeout, default is 10s
     */
    @Value("${odc.rpc.read-timeout-seconds:60}")
    private long readTimeoutSeconds = 60;
}
