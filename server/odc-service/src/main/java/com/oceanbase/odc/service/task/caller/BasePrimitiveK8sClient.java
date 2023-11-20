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

package com.oceanbase.odc.service.task.caller;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public abstract class BasePrimitiveK8sClient implements K8sClient {

    public BasePrimitiveK8sClient(String k8sClusterUrl) throws IOException {
        ApiClient apiClient = Config.defaultClient();
        apiClient.setHttpClient(apiClient
                .getHttpClient()
                .newBuilder()
                .readTimeout(Duration.ZERO)
                .pingInterval(1, TimeUnit.MINUTES)
                .build())
                .setBasePath(k8sClusterUrl);
        Configuration.setDefaultApiClient(apiClient);
    }

    protected abstract String getVersion();

    protected abstract String getKind();

}
