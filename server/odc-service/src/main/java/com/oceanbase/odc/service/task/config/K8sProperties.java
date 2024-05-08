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
package com.oceanbase.odc.service.task.config;

import lombok.Data;

/**
 * @author yaobin
 * @date 2024-01-10
 * @since 4.2.4
 */
@Data
public class K8sProperties {

    private String kubeUrl;
    private String namespace;
    private String kubeConfig;
    private String region;
    /**
     * pod image name with version, odc job will be running in this image
     */
    private String podImageName;
    /**
     * pod pending timeout
     */
    private Long podPendingTimeoutSeconds;

    /**
     * pod request cpu
     */
    private Double requestCpu;

    /**
     * pod request memory
     */
    private Long requestMem;

    /**
     * pod limit cpu
     */
    private Double limitCpu;

    /**
     * pod limit memory
     */
    private Long limitMem;

    /**
     * pod enable mount
     */
    private Boolean enableMount;
    /**
     * pod mount disk absolute path
     */
    private String mountPath;

    /**
     * pod mount disk size
     */
    private Long mountDiskSize;

    /**
     * max node count in pool
     */
    private Long maxNodeCount;

    /**
     * node cpu
     */
    private Double nodeCpu;

    /**
     * node memory
     */
    private Long nodeMemInMB;

}
