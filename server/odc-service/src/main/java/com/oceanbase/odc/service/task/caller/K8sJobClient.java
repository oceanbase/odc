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

import java.util.List;
import java.util.Optional;

import com.oceanbase.odc.service.task.exception.JobException;

/**
 * K8sJobClient is a client to CRUD k8s job in different environment. eg: native or cloud k8s
 *
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public interface K8sJobClient {

    /**
     * create job in k8s namespace with pod name and use a specific image
     *
     * @param namespace namespace name
     * @param name pod name
     * @param image image name
     * @param command image start command
     * @param podConfig pod config
     * @return arn string
     * @throws JobException throws exception when create job failed
     */
    String create(String namespace, String name, String image, List<String> command,
            PodConfig podConfig) throws JobException;

    /**
     * get job by serial number in k8s namespace
     *
     * @param namespace namespace name
     * @param arn arn string
     * @return job serial number
     * @throws JobException throws exception when get job failed
     */
    Optional<K8sJobResponse> get(String namespace, String arn) throws JobException;

    /**
     * delete job by serial number in k8s namespace
     *
     * @param namespace namespace name
     * @param arn arn string
     * @return job serial number
     * @throws JobException throws exception when delete job failed
     */
    String delete(String namespace, String arn) throws JobException;
}
