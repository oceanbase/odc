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

package com.oceanbase.odc.core.task.context;

import java.util.List;

import com.oceanbase.odc.core.session.ConnectionConfig;

import lombok.Builder;
import lombok.Data;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */

@Builder
@Data
public class JobContext {

    /**
     * task id
     */
    private Long taskId;
    /**
     * task param
     */
    private TaskParam taskParam;
    /**
     * connection config information
     */
    private ConnectionConfig connectionConfig;
    /**
     * odc server host properties
     */
    private List<HostProperties> hostProperties;
}
