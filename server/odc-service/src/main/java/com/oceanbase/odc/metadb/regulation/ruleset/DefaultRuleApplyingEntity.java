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

package com.oceanbase.odc.metadb.regulation.ruleset;

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

import com.oceanbase.odc.common.jpa.JsonListConverter;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: Lebie
 * @Date: 2023/12/4 17:18
 * @Description: []
 */
@Data
@Entity
@Table(name = "regulation_default_rule_applying")
@EqualsAndHashCode(exclude = {"id", "createTime", "updateTime"})
public class DefaultRuleApplyingEntity {
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

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "ruleset_name", nullable = false)
    private String rulesetName;

    @Column(name = "rule_metadata_id", nullable = false)
    private Long ruleMetadataId;

    @Column(name = "applied_dialect_types")
    @Convert(converter = JsonListConverter.class)
    private List<String> appliedDialectTypes;

    @Column(name = "properties_json", nullable = false)
    private String propertiesJson;
}
