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
package com.oceanbase.odc.metadb.llm;

import java.util.Date;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.llm.model.ModelType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * LLM integration entity
 */
@Data
@Entity
@Table(name = "integration_llm_model")
public class LlmModelEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "last_modifier_id")
    private Long lastModifierId;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false)
    private ModelType modelType;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "is_deprecated", nullable = false)
    private Boolean deprecated;

    @Column(name = "is_custom", nullable = false)
    private Boolean custom;

    @Column(name = "is_function_calling_support", nullable = false)
    private Boolean functionCallingSupport;

    @Column(name = "properties_json")
    private String propertiesJson;

    @Column(name = "salt")
    private String salt;

    @Column(name = "max_token")
    private Integer maxToken;

    @Column(name = "used_token")
    private Long usedToken;

    @Column(name = "context_size")
    private Integer contextSize;

    @Column(name = "description")
    private String description;
}
