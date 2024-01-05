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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.common.jpa.ListConverter;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 21:26
 * @Description: []
 */
@Data
@Entity
@Table(name = "notification_policy")
public class NotificationPolicyEntity {
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
    @Column(name = "title_template")
    private String titleTemplate;
    @Column(name = "content_template")
    private String contentTemplate;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "policy_metadata_id", nullable = false)
    private Long policyMetadataId;
    @Column(name = "match_expression_json", nullable = false)
    private String matchExpression;
    @Convert(converter = ListConverter.class)
    @Column(name = "to_users")
    private List<String> toUsers;
    @Convert(converter = ListConverter.class)
    @Column(name = "cc_users")
    private List<String> ccUsers;
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;
}
