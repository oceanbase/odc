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
package com.oceanbase.odc.service.audit.model;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.AuditEventType;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/2/28 下午2:19
 * @Description: []
 */
@Data
public class AuditEventExportReq {
    private List<AuditEventType> eventTypes;
    private List<AuditEventAction> eventActions;
    private List<Long> userIds;
    private List<Long> connectionIds;
    private List<AuditEventResult> results;
    @NotNull
    private DownloadFormat format;
    private Date startTime;
    private Date endTime;
}
