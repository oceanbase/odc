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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.core.shared.constant.OrganizationType;

import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "iam_organization")
@ToString(exclude = "secret")
public class OrganizationEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Record insertion time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private Date createTime;

    /**
     * Record modification time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    /**
     * Unique identifier, may from external system (e.g. a GUID of main account in public aliyun), <br>
     * or UUID generated by ODC if no external identifier
     */
    @Column(name = "unique_identifier", nullable = false)
    private String uniqueIdentifier;

    /**
     * Name
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Display name
     */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * Secret for public connection encryption
     */
    @Column(name = "secret", nullable = false)
    private String secret;

    /**
     * UserID of creator, may NULL
     */
    @Column(name = "creator_id")
    private Long creatorId;

    /**
     * Description
     */
    @Column(name = "description")
    private String description;

    /**
     * Built in
     */
    @Column(name = "is_builtin", nullable = false)
    private Boolean builtIn;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OrganizationType type;

}
