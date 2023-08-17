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
package com.oceanbase.odc.service.regulation.ruleset.model;

import java.util.List;

import com.oceanbase.odc.common.i18n.Internationalizable;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/5/17 19:48
 * @Description: []
 */
@Data
public class PropertyMetadata {
    private String name;

    @Internationalizable
    private String displayName;

    @Internationalizable
    private String description;

    private PropertyType type;

    private PropertyInteractiveComponentType componentType;

    /**
     * the concrete type depends on type
     */
    private Object defaultValue;

    /**
     * the alternatives, only meaningful when type is SINGLE_CHOICE or MULTi_CHOICES
     */
    private List<Object> candidates;

}
