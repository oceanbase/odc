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
import static com.oceanbase.odc.service.task.caller.JobConstants.TEMPLATE_BATH_API_VERSION;
import static com.oceanbase.odc.service.task.caller.JobConstants.TEMPLATE_KIND_JOB;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public class PrimitiveJobBasedK8sClient extends BasePrimitiveK8sClient {

    public PrimitiveJobBasedK8sClient(String k8sClusterUrl) throws IOException {
        super(k8sClusterUrl);
    }

    @Override
    public String createNamespaceJob(@NonNull String namespace, @NonNull String jobName, @NonNull String image,
            @NonNull List<String> command, @NonNull PodParam podParam) throws JobException {
        V1Job job = getV1Job(jobName, image, command, podParam);
        BatchV1Api api = new BatchV1Api();
        try {
            V1Job createdJob = api.createNamespacedJob(namespace, job, null, null,
                    null, null);
            return createdJob.getMetadata().getName();
        } catch (ApiException e) {
            throw new JobException(e.getResponseBody(), e);
        }
    }

    @Override
    public Optional<String> getNamespaceJob(@NonNull String namespace, @NonNull String jobName) throws JobException {
        BatchV1Api api = new BatchV1Api();
        V1JobList job = null;
        try {
            job = api.listNamespacedJob(namespace, null, null, null,
                    FIELD_SELECTOR_METADATA_NAME + "=" + jobName,
                    null, null, null, null, null, null);
        } catch (ApiException e) {
            throw new JobException(e.getResponseBody(), e);
        }
        Optional<V1Job> v1Job = job.getItems().stream().findAny();
        return v1Job.map(value -> value.getMetadata().getName());
    }

    @Override
    public String destroyNamespaceJob(@NonNull String namespace, @NonNull String jobName) throws JobException {
        throw new UnsupportedException();
    }

    private V1Job getV1Job(String jobName, String image, List<String> command, PodParam podParam) {
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

        V1JobSpec jobSpec = new V1JobSpec()
                .ttlSecondsAfterFinished(podParam.getTtlSecondsAfterFinished())
                .template(new V1PodTemplateSpec().spec(v1PodSpec));

        return new V1Job()
                .apiVersion(getVersion()).kind(getKind())
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(jobSpec);
    }

    @Override
    protected String getVersion() {
        return TEMPLATE_BATH_API_VERSION;
    }

    @Override
    protected String getKind() {
        return TEMPLATE_KIND_JOB;
    }
}
