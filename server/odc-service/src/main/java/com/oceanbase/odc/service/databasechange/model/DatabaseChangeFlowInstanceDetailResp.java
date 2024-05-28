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
package com.oceanbase.odc.service.databasechange.model;

import java.util.Date;

import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: zijia.cj
 * @date: 2024/5/20
 */
@Data
@NoArgsConstructor
public class DatabaseChangeFlowInstanceDetailResp {
    private Long id;
    private Date createTime;
    private Date executionTime;
    private Date completeTime;
    private double progressPercentage;

    public DatabaseChangeFlowInstanceDetailResp(FlowInstanceDetailResp flowInstanceDetailResp) {
        if (flowInstanceDetailResp != null) {
            this.id = flowInstanceDetailResp.getId();
            this.createTime = flowInstanceDetailResp.getCreateTime();
            this.executionTime = flowInstanceDetailResp.getExecutionTime();
            this.completeTime = flowInstanceDetailResp.getCompleteTime();
            this.progressPercentage = flowInstanceDetailResp.getProgressPercentage();
        }
    }
}
