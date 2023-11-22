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

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public interface JobContext {

    /**
     * get task id
     */
    Long getTaskId();

    /**
     * get task type
     */
    TaskType getTaskType();

    /**
     * get task parameters
     */
    TaskParameters getTaskParameters();

    /**
     * get task connection config
     */
    List<ConnectionConfig> getConnectionConfigs();

    /**
     * get odc server host properties
     */
    List<HostProperties> getHostProperties();
}
