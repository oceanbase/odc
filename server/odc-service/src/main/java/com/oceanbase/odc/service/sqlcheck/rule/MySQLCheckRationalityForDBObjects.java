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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/6/12 19:25
 * @since: 4.3.4
 */
public class MySQLCheckRationalityForDBObjects implements SqlCheckRule {
    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.CHECK_RATIONALITY_FOR_DB_OBJECTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        String text = statement.getText();
        if (statement.getClass() == Select.class) {
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            List<Projection> selectItems = selectBody.getSelectItems();
            List<String> shouldExistedColumns = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(selectItems)) {

                for (Projection selectItem : selectItems) {
                    if (selectItem.isStar()) {
                        continue;
                    }
                    if (null != selectItem.getColumn()) {
                        ColumnReference column = (ColumnReference) selectItem.getColumn();
                        String column1 = column.getColumn();

                    }
                }
                shouldExistedColumns.addAll(selectItems.stream().map(Projection::getText).collect(Collectors.toList()));
            }

            List<String> shouldNotExistedColumns = new ArrayList<>();

        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL);
    }
}
