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
package com.oceanbase.odc.service.notification.model;

import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 14:50
 * @Description: []
 */
@Data
public class NotificationPolicy {

    private long id;
    private Date createTime;
    private Date updateTime;
    private Long creatorId;
    private Long organizationId;
    private Long projectId;
    private String matchExpression;
    private boolean enabled;
    private List<Channel> channels;
    private Long policyMetadataId;
    private String eventName;
    private List<String> toUsers;
    private List<String> ccUsers;

}
