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
package com.oceanbase.odc.metadb.iam;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.print.DocFlavor.STRING;

import org.hibernate.annotations.Where;

import com.oceanbase.odc.core.shared.PermissionConfiguration;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author wenniu.ly
 * @date 2021/7/1
 */

@Data
@Entity
@EqualsAndHashCode(exclude = {"updateTime", "createTime"})
@Where(clause = "expire_time > now()")
@Table(name = "iam_permission")
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEntity implements PermissionConfiguration {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false)
    private String action;
    /**
     * resourceIdentifier consists of resource category [like public_connection / resource_groups / user
     * / role / system_config] and resource id [sometimes maybe name]
     */
    @Column(name = "resource_identifier", nullable = false)
    private String resourceIdentifier;

    @Enumerated(value = EnumType.STRING)
    private PermissionType type;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "is_builtin", nullable = false)
    private Boolean builtIn;

    @Column(name = "expire_time", nullable = false)
    private Date expireTime;

    @Column(name = "authorization_type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private AuthorizationType authorizationType;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Override
    public String resourceIdentifier() {
        return this.resourceIdentifier;
    }

    @Override
    public Set<String> actions() {
        return new HashSet<>(Collections.singletonList(this.action));
    }
}
