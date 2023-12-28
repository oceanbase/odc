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
import com.oceanbase.odc.common.json.NormalDialectTypeOutput;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/5/19 16:43
 * @Description: []
 */
@Data
public class RuleMetadata {
    private Long id;

    @Internationalizable
    private String name;

    @Internationalizable
    private String description;

    /**
     * distinguish between SQL_CHECK rule and SQL_CONSOLE rule
     */
    private RuleType type;

    /**
     * if type = SQL_CHECK, then subType may be DDL, DML, DQL, TABLE, INDEX, SELECT, UPDATE etc.<br>
     * if type = SQL_CONSOLE, then subType may be SECURITY, PERMISSION, etc.<br>
     */
    private List<String> subTypes;

    /**
     * represents which dialectTypes that the rule COULD be applied to<br>
     * if type = SQL_CONSOLE, then appliedDialectTypes is always null which means it could be applied to
     * all dialectTypes<br>
     */
    @NormalDialectTypeOutput
    private List<DialectType> supportedDialectTypes;


    /**
     * rule value metadata list, describe what the rule value looks like
     */
    @Internationalizable
    private List<PropertyMetadata> propertyMetadatas;

    private Boolean builtIn;
}
