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
package com.oceanbase.odc.service.sqlcheck;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleType;
import com.oceanbase.odc.service.sqlcheck.factory.ProhibitedDatatypeExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooManyInExpressionFactory;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.SqlCheckRules;

/**
 * Test cases for {@link SqlCheckRules}
 *
 * @author yh263208
 * @date 2023-06-07 13:52
 * @since ODC_release_4.2.0
 */
public class SqlCheckRulesTest {

    @Test
    public void createByRule_noParametersRule_createSucceed() {
        Rule rule = createRule(SqlCheckRuleType.INDEX_COLUMN_CALCULATION, null, null);
        SqlCheckRule actual = SqlCheckRules.createByRule(null, DialectType.OB_ORACLE, rule);
        Assert.assertNotNull(actual);
    }

    @Test
    public void createByRule_withParametersRule_createSucceed() {
        TooManyInExpressionFactory factory = new TooManyInExpressionFactory();
        Map<String, Object> parameter = new HashMap<>();
        parameter.put(factory.getParameterNameKey("max-in-expr-count"), 12345);
        Rule rule = createRule(SqlCheckRuleType.TOO_MANY_IN_EXPR, parameter, null);
        SqlCheckRule actual = SqlCheckRules.createByRule(null, DialectType.OB_MYSQL, rule);
        Assert.assertNotNull(actual);
    }

    @Test
    public void createByRule_supportNonDialect_createSucceed() {
        TooManyInExpressionFactory factory = new TooManyInExpressionFactory();
        Map<String, Object> parameter = new HashMap<>();
        parameter.put(factory.getParameterNameKey("max-in-expr-count"), 12345);
        Rule rule = createRule(SqlCheckRuleType.TOO_MANY_IN_EXPR, parameter, null);
        SqlCheckRule actual = SqlCheckRules.createByRule(null, DialectType.OB_ORACLE, rule);
        assert actual != null;
        Assert.assertTrue(actual.getSupportsDialectTypes().isEmpty());
    }

    @Test
    public void createByRule_constructBySet_createSucceed() {
        SqlCheckRuleFactory factory = new ProhibitedDatatypeExistsFactory();
        Rule rule = new Rule();
        RuleMetadata metadata = new RuleMetadata();
        metadata.setName("${" + factory.getSupportsType().getName() + "}");
        metadata.setType(RuleType.SQL_CHECK);
        rule.setMetadata(metadata);
        Map<String, Object> parameter = new HashMap<>();
        parameter.putIfAbsent(factory.getParameterNameKey("datatype-names"), Arrays.asList("varchar2", "blob"));
        rule.setProperties(JsonUtils.fromJsonMap(JsonUtils.toJson(parameter), String.class, Object.class));
        SqlCheckRule actual = SqlCheckRules.createByRule(null, DialectType.OB_ORACLE, rule);
        assert actual != null;
        Assert.assertEquals(factory.getSupportsType(), actual.getType());
    }

    @Test
    public void createByRule_supportOBMySQL_createSucceed() {
        TooManyInExpressionFactory factory = new TooManyInExpressionFactory();
        Map<String, Object> parameter = new HashMap<>();
        parameter.put(factory.getParameterNameKey("max-in-expr-count"), 12345);
        Rule rule = createRule(SqlCheckRuleType.TOO_MANY_IN_EXPR, parameter,
                Collections.singletonList(DialectType.OB_MYSQL));
        SqlCheckRule actual = SqlCheckRules.createByRule(null, DialectType.OB_MYSQL, rule);
        assert actual != null;
        Assert.assertEquals(Collections.singletonList(DialectType.OB_MYSQL), actual.getSupportsDialectTypes());
    }

    private Rule createRule(SqlCheckRuleType type, Object parameter, List<DialectType> dialectTypes) {
        Rule rule = new Rule();
        RuleMetadata metadata = new RuleMetadata();
        metadata.setName("${" + type.getName() + "}");
        metadata.setType(RuleType.SQL_CHECK);
        rule.setMetadata(metadata);
        if (parameter != null) {
            rule.setProperties(JsonUtils.fromJsonMap(JsonUtils.toJson(parameter), String.class, Object.class));
        }
        rule.setAppliedDialectTypes(dialectTypes);
        return rule;
    }

}
