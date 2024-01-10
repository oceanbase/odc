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

package com.oceanbase.odc.service.task.enums;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
public enum TaskRunModeEnum {

    /**
     * ODC job run will run in old model and will not be by scheduled task-framework
     */
    LEGACY,

    /**
     * ODC job run by k8s job
     */
    K8S,
    /**
     * ODC job run by ODC server process
     */
    PROCESS;

    public boolean isK8s() {
        return this == K8S;
    }
}
