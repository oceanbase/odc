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
package com.oceanbase.odc.metadb.audit;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author: Lebie
 * @Date: 2022/1/17 下午7:37
 * @Description: []
 */
@Getter
@Setter
@Entity
@EqualsAndHashCode(exclude = {"createTime", "updateTime"})
@Table(name = "audit_event_meta")
public class AuditEventMetaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * record create time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private Date createTime;

    /**
     * record last update time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", nullable = false, insertable = false, updatable = false)
    private Date updateTime;

    /**
     * method signature to audit
     */
    @Column(name = "method_signature", nullable = false)
    private String methodSignature;

    /**
     * Audit event type
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AuditEventType type;

    /**
     * Audit event action
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditEventAction action;

    /**
     * sid extract expression which is an SpEL
     */
    @Column(name = "sid_extract_expression", nullable = false)
    private String sidExtractExpression;

    /**
     * database_id extract expression which is an SpEL
     */
    @Column(name = "database_id_extract_expression")
    private String databaseIdExtractExpression;

    /**
     * Flag if this event is in connection
     */
    @Column(name = "is_in_connection", nullable = false)
    private Boolean inConnection;

    /**
     * Flag if this event enabled
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;
}
