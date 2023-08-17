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
package com.oceanbase.odc.metadb.integration;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.oceanbase.odc.service.integration.model.Encryption.EncryptionAlgorithm;
import com.oceanbase.odc.service.integration.model.IntegrationType;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/3/23 19:07
 */
@Data
@Entity
@Table(name = "integration_integration")
public class IntegrationEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private IntegrationType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "is_builtin", nullable = false)
    private Boolean builtin;

    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "encrypted", nullable = false)
    private Boolean encrypted;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "algorithm", nullable = false)
    private EncryptionAlgorithm algorithm;

    @Column(name = "secret")
    private String secret;

    @Column(name = "salt")
    private String salt;

    @Column(name = "configuration", nullable = false)
    private String configuration;

    @Column(name = "description")
    private String description;
}
