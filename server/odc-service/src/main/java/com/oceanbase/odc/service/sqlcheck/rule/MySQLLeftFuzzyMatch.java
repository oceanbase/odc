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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

/**
 * {@link MySQLLeftFuzzyMatch}
 *
 * @author yh263208
 * @date 2022-12-26 17:32
 * @since ODC_release_4.1.0
 * @see BaseLeftFuzzyMatch
 */
public class MySQLLeftFuzzyMatch extends BaseLeftFuzzyMatch {

    @Override
    protected boolean containsLeftFuzzy(Expression expr) {
        List<String> likes = new ArrayList<>();
        if (expr instanceof ConstExpression) {
            // like/not like 'xxx'
            likes.add(((ConstExpression) expr).getExprConst());
        } else if (expr instanceof CollectionExpression) {
            // like/not like 'xxx' 'xxx' ...
            likes.addAll(((CollectionExpression) expr).getExpressionList()
                    .stream().map(Statement::getText).collect(Collectors.toList()));
        } else if (expr instanceof CompoundExpression) {
            // like/not like 'xxx' escape 'xxx'
            return containsLeftFuzzy(((CompoundExpression) expr).getLeft());
        }
        return likes.stream().map(s -> StringUtils.unwrap(s, "'")).anyMatch(s -> s.startsWith("%"));
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
