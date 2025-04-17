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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.notification.model.ChannelType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 21:19
 * @Description: []
 */
@Data
@Entity
@Table(name = "notification_channel")
public class ChannelEntity {
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
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "name", nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ChannelType type;
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    List<ChannelPropertyEntity> properties;
    @Column(name = "description")
    private String description;
}
