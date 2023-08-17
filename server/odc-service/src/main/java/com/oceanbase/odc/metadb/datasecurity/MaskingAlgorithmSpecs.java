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

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithmType;

/**
 * @author gaoda.xy
 * @date 2023/5/17 13:40
 */
public class MaskingAlgorithmSpecs {
    private static final String MASKING_ALGORITHM_NAME = "name";
    private static final String MASKING_ALGORITHM_TYPE = "type";
    private static final String MASKING_ALGORITHM_ORGANIZATION_ID = "organizationId";

    public static Specification<MaskingAlgorithmEntity> nameLike(String name) {
        return SpecificationUtil.columnLike(MASKING_ALGORITHM_NAME, name);
    }

    public static Specification<MaskingAlgorithmEntity> typeIn(Collection<MaskingAlgorithmType> types) {
        return SpecificationUtil.columnIn(MASKING_ALGORITHM_TYPE, types);
    }

    public static Specification<MaskingAlgorithmEntity> organizationIdEqual(Long id) {
        return SpecificationUtil.columnEqual(MASKING_ALGORITHM_ORGANIZATION_ID, id);
    }

}
