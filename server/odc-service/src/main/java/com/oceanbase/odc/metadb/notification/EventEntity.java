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
package com.oceanbase.odc.metadb.notification;

import java.util.Date;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.notification.model.EventStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/3/21 10:48
 * @Description: []
 */
@Data
@Entity
@Table(name = "notification_event")
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
    @Column(name = "creator_id", nullable = false, updatable = false)
    private Long creatorId;
    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;
    @Column(name = "trigger_time", nullable = false, updatable = false)
    private Date triggerTime;
    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;
    @OneToMany(mappedBy = "event", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    List<EventLabelEntity> labels;
}
