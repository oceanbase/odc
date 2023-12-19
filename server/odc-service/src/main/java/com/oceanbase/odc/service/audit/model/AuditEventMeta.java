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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventType;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/1/18 下午5:37
 * @Description: []
 */
@Data
@Builder
public class AuditEventMeta {
    /**
     * id of the event meta
     */
    private Long id;

    /**
     * method signature to audit
     */
    private String methodSignature;

    /**
     * Audit event type
     */
    private AuditEventType type;

    /**
     * Audit event action
     */
    private AuditEventAction action;

    /**
     * sid extract expression which is an SpEL
     */
    private String sidExtractExpression;

    /**
     * database id extract expression which is an SpEL
     */
    private String databaseIdExtractExpression;

    /**
     * Flag if this event is in connection
     */
    @JsonProperty("isInConnection")
    @Deprecated
    private Boolean inConnection;

    /**
     * Flag if this event enabled
     */
    private Boolean enabled;
}
