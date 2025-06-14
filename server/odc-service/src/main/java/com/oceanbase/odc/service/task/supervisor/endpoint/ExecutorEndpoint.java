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
package com.oceanbase.odc.service.task.supervisor.endpoint;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author longpeng.zlp
 * @date 2024/10/28 16:41
 */
@Data
@AllArgsConstructor
public class ExecutorEndpoint {
    private String protocol;
    private String host;
    private Integer supervisorPort;
    // owner of supervisor to run other protocol, may be null, if not provided
    private Integer supervisorOwnerPort;
    private Integer executorPort;
    private String identifier;

    public ExecutorEndpoint() {}
}
