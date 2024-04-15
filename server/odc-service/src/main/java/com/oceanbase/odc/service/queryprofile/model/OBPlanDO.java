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
package com.oceanbase.odc.service.queryprofile.model;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/12
 */
@Data
public class OBPlanDO {

    private String id;
    private String parentId;
    private String operator;
    private String objectOwner;
    private String objectName;
    private String objectAlias;
    private String other;
    private String accessPredicates;
    private String filterPredicates;
    private String projection;
    private String specialPredicates;

}
