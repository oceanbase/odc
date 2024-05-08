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

import static com.oceanbase.odc.service.task.constants.JobConstants.FIELD_SELECTOR_METADATA_NAME;
import static com.oceanbase.odc.service.task.constants.JobConstants.RESTART_POLICY_NEVER;
import static com.oceanbase.odc.service.task.constants.JobConstants.TEMPLATE_API_VERSION;
import static com.oceanbase.odc.service.task.constants.JobConstants.TEMPLATE_KIND_POD;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.EncodeUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.exception.JobException;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public class NativeK8sJobClient implements K8sJobClient {

    private final K8sProperties k8sProperties;
    private static final long TIMEOUT_MILLS = 60000;

    public NativeK8sJobClient(K8sProperties k8sProperties) throws IOException {
        this.k8sProperties = k8sProperties;
        ApiClient apiClient = null;
        if (StringUtils.isNotBlank(k8sProperties.getKubeConfig())) {
            byte[] kubeConfigBytes = EncodeUtils.base64DecodeFromString(k8sProperties.getKubeConfig());
            Verify.notNull(kubeConfigBytes, "kube config");
            try (Reader targetReader = new InputStreamReader(new ByteArrayInputStream(kubeConfigBytes))) {
                KubeConfig kubeConfig = KubeConfig.loadKubeConfig(targetReader);
                apiClient = ClientBuilder.kubeconfig(kubeConfig).build();
            }
        } else if (StringUtils.isNotBlank(k8sProperties.getKubeUrl())) {
            apiClient = Config.defaultClient().setBasePath(k8sProperties.getKubeUrl());
        }
        Verify.notNull(apiClient, "k8s api client");
        apiClient.setHttpClient(apiClient
                .getHttpClient()
                .newBuilder()
                .readTimeout(TIMEOUT_MILLS, TimeUnit.MILLISECONDS)
                .connectTimeout(TIMEOUT_MILLS, TimeUnit.MILLISECONDS)
                .pingInterval(1, TimeUnit.MINUTES)
                .build());
        Configuration.setDefaultApiClient(apiClient);
    }

    @Override
    public String create(@NonNull String namespace, @NonNull String name, @NonNull String image,
            List<String> command, @NonNull PodConfig podConfig) throws JobException {
        validK8sProperties();

        V1Pod job = getV1Pod(name, image, command, podConfig);
        CoreV1Api api = new CoreV1Api();
        try {
            V1Pod createdJob = api.createNamespacedPod(namespace, job, null, null,
                    null, null);
            return createdJob.getMetadata().getName();
        } catch (ApiException e) {
            if (e.getResponseBody() != null) {
                throw new JobException(e.getResponseBody(), e);
            } else {
                throw new JobException("Create job occur error:", e);
            }
        }
    }

    @Override
    public Optional<K8sJobResponse> get(@NonNull String namespace, @NonNull String arn) throws JobException {
        validK8sProperties();
        CoreV1Api api = new CoreV1Api();
        V1PodList job = null;
        try {
            job = api.listNamespacedPod(namespace, null, null, null,
                    FIELD_SELECTOR_METADATA_NAME + "=" + arn,
                    null, null, null, null, null, null, false);
        } catch (ApiException e) {
            throw new JobException(e.getResponseBody(), e);
        }
        Optional<V1Pod> v1PodOptional = job.getItems().stream().findAny();
        if (!v1PodOptional.isPresent()) {
            return Optional.empty();
        }
        V1Pod v1Pod = v1PodOptional.get();
        DefaultK8sJobResponse response = new DefaultK8sJobResponse();
        response.setArn(arn);
        response.setName(arn);
        response.setRegion(k8sProperties.getRegion());
        response.setResourceStatus(v1Pod.getStatus().getPhase());
        return Optional.of(response);
    }

    @Override
    public String delete(@NonNull String namespace, @NonNull String arn) throws JobException {
        validK8sProperties();
        CoreV1Api api = new CoreV1Api();
        V1Pod pod = null;
        try {
            pod = api.deleteNamespacedPod(arn, namespace, null, null,
                    null, null, null, null);
        } catch (ApiException e) {
            throw new JobException(e.getResponseBody(), e);
        }
        return pod.getMetadata().getName();
    }

    private V1Pod getV1Pod(String jobName, String image, List<String> command, PodConfig podParam) {
        V1Container container = new V1Container()
                .name(jobName)
                .image(image)
                .imagePullPolicy(podParam.getImagePullPolicy());

        if (CollectionUtils.isNotEmpty(command)) {
            container.setCommand(command);
        }

        if (podParam.getEnvironments().size() > 0) {
            List<V1EnvVar> envVars = podParam.getEnvironments().entrySet().stream()
                    .map(entry -> new V1EnvVar().name(entry.getKey()).value(entry.getValue()))
                    .collect(Collectors.toList());
            container.setEnv(envVars);
        }

        V1PodSpec v1PodSpec = new V1PodSpec()
                .containers(Collections.singletonList(container))
                .restartPolicy(RESTART_POLICY_NEVER);

        return new V1Pod()
                .apiVersion(getVersion()).kind(getKind())
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(v1PodSpec);
    }

    private void validK8sProperties() {
        PreConditions.validArgumentState(k8sProperties.getKubeConfig() != null
                || k8sProperties.getKubeUrl() != null,
                ErrorCodes.BadArgument,
                new Object[] {}, "Target k8s is not set");
    }

    private String getVersion() {
        return TEMPLATE_API_VERSION;
    }

    private String getKind() {
        return TEMPLATE_KIND_POD;
    }
}
