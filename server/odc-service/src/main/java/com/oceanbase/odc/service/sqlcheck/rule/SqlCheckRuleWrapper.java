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
package com.oceanbase.odc.service.sqlcheck.rule;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;

/**
 * {@link SqlCheckRuleWrapper}
 *
 * @author yh263208
 * @date 2023-06-07 11:50
 * @since ODC_release_4.2.0
 */
class SqlCheckRuleWrapper implements SqlCheckRule {

    private final SqlCheckRule target;
    private final List<DialectType> supports;

    public SqlCheckRuleWrapper(@NonNull SqlCheckRule target, List<DialectType> supports) {
        this.target = target;
        this.supports = supports;
    }

    @Override
    public SqlCheckRuleType getType() {
        return this.target.getType();
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        return this.target.check(statement, context);
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return this.target.getSupportsDialectTypes().stream()
                .filter(d -> !CollectionUtils.isEmpty(supports) && supports.contains(d))
                .collect(Collectors.toList());
    }

}
