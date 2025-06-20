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
package com.oceanbase.odc.service.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchTerminateFlowResult {
    private Boolean terminateSucceed;
    private Long flowInstanceId;
    private String failReason;

    public static BatchTerminateFlowResult success(Long id) {
        return new BatchTerminateFlowResult(true, id, null);
    }

    public static BatchTerminateFlowResult failed(Long id, String failReason) {
        return new BatchTerminateFlowResult(false, id, failReason);

    }

}
