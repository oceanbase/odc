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
package com.oceanbase.odc.metadb.flow;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;

/**
 * {@link org.springframework.data.jpa.domain.Specification} for {@link GateWayInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-08 20:34
 * @since ODC_release_3.3.0
 */
public class GateWayInstanceSpecs {

    private static final String GATEWAY_ID_NAME = "id";
    private static final String GATEWAY_ORGANIZATION_ID_NAME = "organizationId";
    private static final String GATEWAY_FLOW_INSTANCE_NAME = "flowInstanceId";

    public static Specification<GateWayInstanceEntity> idEquals(Long id) {
        return SpecificationUtil.columnEqual(GATEWAY_ID_NAME, id);
    }

    public static Specification<GateWayInstanceEntity> flowInstanceIdEquals(Long id) {
        return SpecificationUtil.columnEqual(GATEWAY_FLOW_INSTANCE_NAME, id);
    }

    public static Specification<GateWayInstanceEntity> organizationIdEquals(Long organizationId) {
        return SpecificationUtil.columnEqual(GATEWAY_ORGANIZATION_ID_NAME, organizationId);
    }

}
