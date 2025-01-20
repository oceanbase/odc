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

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;

import lombok.Builder;
import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2022/2/14
 */

@Data
@Builder
public class QueryFlowInstanceParams {

    private List<Long> connectionIds;
    private String creator;
    private String databaseName;
    /**
     * 目前搜索框中仅支持搜索id，该值指代搜索框中用户填入的
     */
    private String id;
    private List<FlowStatus> statuses;
    private TaskType type;
    private Date startTime;
    private Date endTime;
    private Boolean createdByCurrentUser;
    private Boolean approveByCurrentUser;
    private Boolean containsAll;
    private Long parentInstanceId;

    private Set<Long> projectIds;
}
