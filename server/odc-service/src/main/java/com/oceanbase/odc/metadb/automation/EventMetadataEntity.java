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
package com.oceanbase.odc.metadb.automation;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import lombok.Data;

@Data
@Entity
@Table(name = "automation_event_metadata")
public class EventMetadataEntity {

    /**
     * Id for trigger event
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Auto triggered event name
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Variables needed by condition
     */
    @Column(name = "variable_names")
    private String variableNames;

    /**
     * Builtin or not
     */
    @Column(name = "is_builtin", nullable = false)
    private Boolean builtin;

    /**
     * Whether this event is hidden for show
     */
    @Column(name = "is_hidden", nullable = false)
    private Boolean hidden;

    /**
     * Description
     */
    @Column(name = "description")
    private String description;

    /**
     * Creator id, references iam_user(id)
     */
    @Column(name = "creator_id", updatable = false, nullable = false)
    private Long creatorId;

    /**
     * Organization id, references iam_organization(id)
     */
    @Column(name = "organization_id", updatable = false, nullable = false)
    private Long organizationId;

    /**
     * Last modifier id, references iam_user(id)
     */
    @Column(name = "last_modifier_id")
    private Long lastModifierId;

    /**
     * Record insertion time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false,
            columnDefinition = "datetime NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createTime;

    /**
     * Record modification time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false,
            columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp updateTime;
}
