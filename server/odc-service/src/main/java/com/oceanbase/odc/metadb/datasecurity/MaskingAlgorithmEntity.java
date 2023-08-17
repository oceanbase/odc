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
package com.oceanbase.odc.metadb.datasecurity;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.common.jpa.JsonListConverter;
import com.oceanbase.odc.core.datamasking.algorithm.Hash.HashType;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithmType;
import com.oceanbase.odc.service.datasecurity.model.MaskingSegmentsType;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/5/12 09:41
 */
@Data
@Entity
@Table(name = "data_security_masking_algorithm")
public class MaskingAlgorithmEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "is_builtin", nullable = false)
    private Boolean builtin;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MaskingAlgorithmType type;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "segments_type")
    private MaskingSegmentsType segmentsType;

    @Column(name = "substitution")
    private String substitution;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "charsets")
    private List<String> charsets;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "hash_type")
    private HashType hashType;

    @Column(name = "is_decimal")
    private Boolean decimal;

    @Column(name = "rounding_precision")
    private Integer precision;

    @Column(name = "sample_content", nullable = false)
    private String sampleContent;

    @Column(name = "masked_content")
    private String maskedContent;

    @Column(name = "creator_id", nullable = false, updatable = false)
    private Long creatorId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;

    @Generated(value = GenerationTime.ALWAYS)
    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private Date createTime;

    @Generated(value = GenerationTime.ALWAYS)
    @Column(name = "update_time", nullable = false, insertable = false, updatable = false)
    private Date updateTime;

}
