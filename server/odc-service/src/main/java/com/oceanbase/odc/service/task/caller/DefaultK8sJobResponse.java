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

import lombok.Data;

/**
 * @author yaobin
 * @date 2024-04-03
 * @since 4.2.4
 */
@Data
public class DefaultK8sJobResponse implements K8sJobResponse {

    /**
     * job region
     */
    private String region;

    /**
     * job identity string
     */
    private String arn;

    /**
     * job alias name
     */
    private String name;

    /**
     * pod status
     */
    private String resourceStatus;

}
