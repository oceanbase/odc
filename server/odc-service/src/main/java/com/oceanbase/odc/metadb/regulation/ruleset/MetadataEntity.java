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
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleType;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: Lebie
 * @Date: 2023/5/18 13:57
 * @Description: []
 */
@Getter
@Setter
@Entity
@Table(name = "regulation_rule_metadata")
public class MetadataEntity {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private RuleType type;

    @Column(name = "is_builtin", nullable = false)
    @JsonProperty("builtIn")
    private Boolean builtIn;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "metadata")
    @LazyCollection(LazyCollectionOption.FALSE)
    @Fetch(FetchMode.SUBSELECT)
    private List<MetadataLabelEntity> labels;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ruleMetadata")
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonProperty("propertyMetadatas")
    @Fetch(FetchMode.SUBSELECT)
    private List<PropertyMetadataEntity> propertyMetadatas;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof MetadataEntity)) {
            return false;
        }
        MetadataEntity that = (MetadataEntity) obj;
        return Objects.equals(this.getId(), that.getId()) && Objects.equals(this.getBuiltIn(), that.getBuiltIn())
                && Objects.equals(this.getName(), that.getName())
                && Objects.equals(this.getDescription(), that.getDescription())
                && Objects.equals(this.getType(), that.getType())
                && ((this.getLabels() != null && that.getLabels() != null
                        && CollectionUtils.isEqualCollection(this.getLabels(), that.getLabels()))
                        || (this.getLabels() == null && that.getLabels() == null))
                && ((this.getPropertyMetadatas() != null && that.getPropertyMetadatas() != null
                        && CollectionUtils.isEqualCollection(this.getPropertyMetadatas(), that.getPropertyMetadatas()))
                        || (this.getPropertyMetadatas() == null && that.getPropertyMetadatas() == null));
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "createTime", "updateTime");
    }
}
