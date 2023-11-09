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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.common.jpa.JsonListConverter;
import com.oceanbase.odc.service.regulation.ruleset.model.PropertyInteractiveComponentType;
import com.oceanbase.odc.service.regulation.ruleset.model.PropertyType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author: Lebie
 * @Date: 2023/5/18 15:16
 * @Description: []
 */


@Getter
@Setter
@Entity
@Table(name = "regulation_rule_metadata_property_metadata")
public class PropertyMetadataEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private PropertyType type;

    @Column(name = "component_type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    @JsonProperty("componentType")
    private PropertyInteractiveComponentType componentType;

    @Column(name = "default_values", nullable = false)
    @Convert(converter = JsonListConverter.class)
    @JsonProperty("defaultValues")
    private List<String> defaultValues = new ArrayList<>();

    @Column(name = "candidates")
    @Convert(converter = JsonListConverter.class)
    private List<String> candidates = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "rule_metadata_id", referencedColumnName = "id")
    @ToString.Exclude
    private MetadataEntity ruleMetadata;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof PropertyMetadataEntity)) {
            return false;
        }
        PropertyMetadataEntity that = (PropertyMetadataEntity) obj;

        return Objects.equals(this.getName(), that.getName()) && Objects.equals(this.getType(), that.getType())
                && Objects.equals(this.getComponentType(), that.getComponentType())
                && Objects.equals(this.getDescription(), that.getDescription())
                && ((CollectionUtils.isEmpty(this.getCandidates()) && CollectionUtils.isEmpty(that.getCandidates())) ||
                        (this.getCandidates() != null && that.getCandidates() != null
                                && CollectionUtils.isEqualCollection(this.getCandidates(), that.getCandidates()))
                        || (this.getCandidates() == null && that.getCandidates() == null))
                && ((CollectionUtils.isEmpty(this.getDefaultValues())
                        && CollectionUtils.isEmpty(that.getDefaultValues())) ||
                        (this.getDefaultValues() != null && that.getDefaultValues() != null
                                && CollectionUtils.isEqualCollection(this.getDefaultValues(), that.getDefaultValues()))
                        || (this.getDefaultValues() == null && that.getDefaultValues() == null));
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "id", "ruleMetadata");
    }

}
