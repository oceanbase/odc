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
package com.oceanbase.odc.service.resource.builder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.util.EncodeUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.resource.model.NativeK8sResourceID;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.NonNull;

/**
 * {@link BaseNativeK8sResourceOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-02 18:06
 * @since ODC_release_4.3.2
 */
public abstract class BaseNativeK8sResourceOperatorBuilder<T extends KubernetesObject>
        implements ResourceOperatorBuilder<T, NativeK8sResourceID> {

    private static final long TIMEOUT_MILLS = 60000;
    private final Object lock = new Object();
    private volatile boolean apiClientAvailable = false;
    private volatile boolean apiClientSet = false;
    @Autowired
    private TaskFrameworkProperties taskFrameworkProperties;

    @PostConstruct
    public void setUp() throws IOException {
        if (this.apiClientSet) {
            return;
        }
        synchronized (this.lock) {
            if (this.apiClientSet) {
                return;
            }
            this.apiClientSet = true;
            K8sProperties properties = this.taskFrameworkProperties.getK8sProperties();
            if (properties == null) {
                return;
            }
            ApiClient apiClient;
            if (StringUtils.isNotBlank(properties.getKubeConfig())) {
                byte[] config = EncodeUtils.base64DecodeFromString(properties.getKubeConfig());
                Verify.notNull(config, "kube config");
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(config))) {
                    KubeConfig kubeConfig = KubeConfig.loadKubeConfig(reader);
                    apiClient = ClientBuilder.kubeconfig(kubeConfig).build();
                }
            } else if (StringUtils.isNotBlank(properties.getKubeUrl())) {
                apiClient = Config.defaultClient().setBasePath(properties.getKubeUrl());
            } else {
                return;
            }
            apiClient.setHttpClient(apiClient
                    .getHttpClient()
                    .newBuilder()
                    .readTimeout(TIMEOUT_MILLS, TimeUnit.MILLISECONDS)
                    .connectTimeout(TIMEOUT_MILLS, TimeUnit.MILLISECONDS)
                    .pingInterval(1, TimeUnit.MINUTES)
                    .build());
            Configuration.setDefaultApiClient(apiClient);
            this.apiClientAvailable = true;
        }
    }

    protected abstract boolean doSupports(@NonNull Class<?> clazz);

    public boolean supports(@NonNull Class<?> clazz) {
        return this.apiClientAvailable && doSupports(clazz);
    }

}
