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

package com.oceanbase.odc.service.notification.helper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.NotificationPolicy;

public class NotificationPolicyFilter {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    public static List<NotificationPolicy> filter(EventLabels labels, List<NotificationPolicyEntity> policies) {
        if (CollectionUtils.isEmpty(policies)) {
            return Collections.emptyList();
        }
        StandardEvaluationContext context = new StandardEvaluationContext(labels);
        context.addPropertyAccessor(new MapAccessor());
        return policies.stream()
                .filter(policy -> evaluateExpression(policy.getMatchExpression(), context))
                .map(PolicyMapper::fromEntity)
                .collect(Collectors.toList());
    }

    private static Boolean evaluateExpression(String expr, EvaluationContext context) {
        Expression expression = PARSER.parseExpression(expr);
        return expression.getValue(context, Boolean.class);
    }

}
