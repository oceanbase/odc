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

import static com.oceanbase.odc.service.task.caller.JobConstants.FIELD_SELECTOR_METADATA_NAME;
import static com.oceanbase.odc.service.task.caller.JobConstants.RESTART_POLICY_NEVER;
import static com.oceanbase.odc.service.task.caller.JobConstants.TEMPLATE_API_VERSION;
import static com.oceanbase.odc.service.task.caller.JobConstants.TEMPLATE_KIND_POD;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public class PrimitivePodBasedK8sClient extends BasePrimitiveK8sClient {

    public PrimitivePodBasedK8sClient(String k8sClusterUrl) throws IOException {
        super(k8sClusterUrl);
    }

    @Override
    public String createNamespaceJob(@NonNull String namespace, @NonNull String jobName, @NonNull String image,
            @NonNull List<String> command, @NonNull PodParam podParam) throws JobException {
        V1Pod job = getV1Pod(jobName, image, command, podParam);
        CoreV1Api api = new CoreV1Api();
        try {
            V1Pod createdJob = api.createNamespacedPod(namespace, job, null, null,
                    null, null);
            return createdJob.getMetadata().getName();
        } catch (ApiException e) {
            throw new JobException(e.getResponseBody(), e);
        }
    }

    @Override
    public Optional<String> getNamespaceJob(@NonNull String namespace, @NonNull String jobName) throws JobException {
        CoreV1Api api = new CoreV1Api();
        V1PodList job = null;
        try {
            job = api.listNamespacedPod(namespace, null, null, null,
                    FIELD_SELECTOR_METADATA_NAME + "=" + jobName,
                    null, null, null, null, null, null);
        } catch (ApiException e) {
            throw new JobException(e.getResponseBody(), e);
        }
        Optional<V1Pod> v1Job = job.getItems().stream().findAny();
        return v1Job.map(value -> value.getMetadata().getName());
    }

    @Override
    public String destroyNamespaceJob(@NonNull String namespace, @NonNull String jobName) throws JobException {
        CoreV1Api api = new CoreV1Api();
        V1Pod pod = null;
        try {
            pod = api.deleteNamespacedPod(jobName, namespace, null, null,
                    null, null, null, null);
        } catch (ApiException e) {
            throw new JobException(e.getResponseBody(), e);
        }
        return pod.getMetadata().getName();
    }

    private V1Pod getV1Pod(String jobName, String image, List<String> command, PodParam podParam) {
        V1Container container = new V1Container()
                .name(jobName)
                .image(image)
                .command(command);

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

    @Override
    protected String getVersion() {
        return TEMPLATE_API_VERSION;
    }

    @Override
    protected String getKind() {
        return TEMPLATE_KIND_POD;
    }
}
