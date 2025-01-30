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
package com.oceanbase.odc.service.task.resource.client;

import static com.oceanbase.odc.service.task.constants.JobConstants.FIELD_SELECTOR_METADATA_NAME;
import static com.oceanbase.odc.service.task.constants.JobConstants.TEMPLATE_API_VERSION;
import static com.oceanbase.odc.service.task.constants.JobConstants.TEMPLATE_KIND_POD;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.oceanbase.odc.common.util.EncodeUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.oceanbase.odc.service.task.resource.K8sResourceContext;
import com.oceanbase.odc.service.task.resource.PodConfig;

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

    // pair left is resource state, right is matching list
    private static final Pair<ResourceState, List<String>>[] K8S_PHASE_MATCHER = new Pair[] {
            Pair.of(ResourceState.CREATING, Arrays.asList("Pending", "INIT")),
            Pair.of(ResourceState.AVAILABLE, Arrays.asList("Running", "ALLOCATED")),
            Pair.of(ResourceState.DESTROYING, Arrays.asList("Terminating", "PENDING_DELETE")),
            Pair.of(ResourceState.UNKNOWN, Arrays.asList("unknown", "UNKNOWN"))
    };

    private final K8sProperties k8sProperties;
    private static final long TIMEOUT_MILLS = 60000;

    public NativeK8sJobClient(K8sProperties k8sProperties) throws IOException {
        this.k8sProperties = k8sProperties;
        ApiClient apiClient = generateNativeK8sApiClient(k8sProperties);
        Verify.notNull(apiClient, "k8s api client");
        Configuration.setDefaultApiClient(apiClient);
    }

    public static ApiClient generateNativeK8sApiClient(
            @NonNull TaskFrameworkProperties taskFrameworkProperties) throws IOException {
        if (taskFrameworkProperties.getK8sProperties() == null) {
            return null;
        }
        return generateNativeK8sApiClient(taskFrameworkProperties.getK8sProperties());
    }

    public static ApiClient generateNativeK8sApiClient(@NonNull K8sProperties properties) throws IOException {
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
            return null;
        }
        apiClient.setHttpClient(apiClient
                .getHttpClient()
                .newBuilder()
                .readTimeout(TIMEOUT_MILLS, TimeUnit.MILLISECONDS)
                .connectTimeout(TIMEOUT_MILLS, TimeUnit.MILLISECONDS)
                .pingInterval(1, TimeUnit.MINUTES)
                .build());
        return apiClient;
    }

    @Override
    public K8sPodResource create(K8sResourceContext k8sResourceContext) throws JobException {
        PodConfig podConfig = k8sResourceContext.getPodConfig();
        return create(k8sResourceContext.resourceNamespace(), k8sResourceContext.resourceName(),
                podConfig.getImage(),
                podConfig.getCommand(), podConfig);
    }

    protected K8sPodResource create(@NonNull String namespace, @NonNull String name, @NonNull String image,
            List<String> command, @NonNull PodConfig podConfig) throws JobException {
        validK8sProperties();

        V1Pod job = getV1Pod(name, image, command, podConfig);
        CoreV1Api api = new CoreV1Api();
        try {
            V1Pod createdJob = api.createNamespacedPod(namespace, job, null, null,
                    null, null);
            // return pod status
            return new K8sPodResource(null, null, null, namespace, createdJob.getMetadata().getName(),
                    k8sPodPhaseToResourceState(createdJob.getStatus().getPhase()),
                    createdJob.getStatus().getPodIP(), String.valueOf(k8sProperties.getExecutorListenPort()),
                    new Date(System.currentTimeMillis() / 1000));
        } catch (ApiException e) {
            Optional<K8sPodResource> existedPod = null;
            if (isPodAlreadyExists(e)) {
                existedPod = get(namespace, name);
            }
            // return existed pod
            if (null != existedPod && existedPod.isPresent()) {
                return existedPod.get();
            }
            if (e.getResponseBody() != null) {
                throw new JobException(e.getResponseBody(), e);
            } else {
                throw new JobException("Create job occur error:", e);
            }
        }
    }

    protected boolean isPodAlreadyExists(ApiException e) {
        return StringUtils.containsIgnoreCase(e.getResponseBody(), "already exists");
    }

    @Override
    public Optional<K8sPodResource> get(@NonNull String namespace, @NonNull String arn) throws JobException {
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
        K8sPodResource resource = new K8sPodResource(k8sProperties.getRegion(), k8sProperties.getGroup(), null,
                namespace, arn, k8sPodPhaseToResourceState(v1Pod.getStatus().getPhase()), v1Pod.getStatus().getPodIP(),
                String.valueOf(k8sProperties.getExecutorListenPort()),
                new Date(System.currentTimeMillis() / 1000));
        return Optional.of(resource);
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
                .restartPolicy("Always");

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

    private ResourceState k8sPodPhaseToResourceState(String k8sPhase) {
        for (Pair<ResourceState, List<String>> matcher : K8S_PHASE_MATCHER) {
            // match each state candidate
            for (String matchStr : matcher.getRight()) {
                if (StringUtils.equalsIgnoreCase(matchStr, k8sPhase)) {
                    return matcher.getLeft();
                }
            }
        }
        return ResourceState.UNKNOWN;
    }
}
