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
package com.oceanbase.odc.service.datasecurity.model;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.validate.Name;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.datamasking.algorithm.Hash.HashType;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmUtil;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/5/9 10:56
 */
@Data
public class MaskingAlgorithm implements SecurityResource, OrganizationIsolated {

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @Size(min = 1, max = 128, message = "Masking algorithm name is out of range [1,128]")
    @Name(message = "Masking algorithm name cannot start or end with whitespaces")
    @Internationalizable
    private String name;

    @NotNull
    private Boolean enabled;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtin;

    @JsonProperty(access = Access.READ_ONLY)
    private InnerUser creator;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @NotNull
    private MaskingAlgorithmType type;

    private MaskingSegmentsType segmentsType;

    private List<MaskingSegment> segments;

    private String substitution;

    private List<String> charsets;

    private HashType hashType;

    private Boolean decimal;

    private Integer precision;

    @NotEmpty
    private String sampleContent;

    private String maskedContent;

    @Override
    public String resourceId() {
        return this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_MASKING_ALGORITHM.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    public void validate() {
        switch (type) {
            case PSEUDO:
                PreConditions.notEmpty(charsets, "Pseudo charsets");
                for (String characterStr : charsets) {
                    if (characterStr.length() > 1) {
                        PreConditions.validRequestState(characterStr.contains(MaskingAlgorithmUtil.RANGE_DELIMITER),
                                ErrorCodes.IllegalArgument, new Object[] {"Pseudo charsets"},
                                "Pseudo charsets range value must have ~ inside");
                    }
                }
                break;
            case HASH:
                PreConditions.notNull(hashType, "Hash type");
                break;
            case MASK:
                if (CollectionUtils.isEmpty(segments)) {
                    PreConditions.notNull(segmentsType, "Mask segments type");
                    PreConditions.validArgumentState(MaskingSegmentsType.CUSTOM != segmentsType,
                            ErrorCodes.IllegalArgument, new Object[] {"Segments"},
                            "Mask segments type cannot be custom while segment list is empty");
                }
                break;
            case SUBSTITUTION:
                if (CollectionUtils.isEmpty(segments)) {
                    PreConditions.notNull(segmentsType, "Mask segments type");
                    PreConditions.validArgumentState(MaskingSegmentsType.CUSTOM != segmentsType,
                            ErrorCodes.IllegalArgument, new Object[] {"Segments"},
                            "Substitution segments type cannot be custom while segment list is empty");
                    PreConditions.notEmpty(substitution, "Substitution replace value");
                }
                break;
            case ROUNDING:
                PreConditions.notNull(precision, "Rounding precision");
                PreConditions.notNull(decimal, "Rounding decimal");
                break;
            case NULL:
            default:
                break;
        }
    }

}
