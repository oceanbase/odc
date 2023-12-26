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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRuleFactory;
import com.oceanbase.odc.service.sqlcheck.factory.ColumnCalculationFactory;
import com.oceanbase.odc.service.sqlcheck.factory.ColumnCharsetExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.ColumnCollationExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.ColumnNameInBlackListFactory;
import com.oceanbase.odc.service.sqlcheck.factory.ForeignConstraintExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.LeftFuzzyMatchFactory;
import com.oceanbase.odc.service.sqlcheck.factory.MissingRequiredColumnsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoColumnCommentExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoDefaultValueExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoIndexNameExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoNotNullAtInExpressionFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoPrimaryKeyExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoPrimaryKeyNameExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoSpecificColumnExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoTableCommentExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoValidWhereClauseFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NoWhereClauseExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.NotNullColumnWithoutDefaultValueFactory;
import com.oceanbase.odc.service.sqlcheck.factory.PreferLocalOutOfLineIndexFactory;
import com.oceanbase.odc.service.sqlcheck.factory.ProhibitedDatatypeExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictAutoIncrementDataTypesFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictAutoIncrementUnsignedFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictColumnNameCaseFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictColumnNotNullFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictDropObjectTypesFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictIndexDataTypesFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictIndexNamingFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictPKAutoIncrementFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictPKDataTypesFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictPKNamingFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictTableAutoIncrementFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictTableCharsetFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictTableCollationFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictTableNameCaseFactory;
import com.oceanbase.odc.service.sqlcheck.factory.RestrictUniqueIndexNamingFactory;
import com.oceanbase.odc.service.sqlcheck.factory.SelectStarExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.SyntaxErrorExistsFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TableNameInBlackListFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooLongCharLengthFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooManyAlterStatementFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooManyColumnDefinitionFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooManyColumnRefInIndexFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooManyColumnRefInPrimaryKeyFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooManyInExpressionFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooManyOutOfLineIndexFactory;
import com.oceanbase.odc.service.sqlcheck.factory.TooManyTableJoinFactory;
import com.oceanbase.odc.service.sqlcheck.factory.ZeroFillExistsFactory;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;

import lombok.NonNull;

/**
 * {@link SqlCheckRules}
 *
 * @author yh263208
 * @date 2023-06-07 11:42
 * @since ODC_release_4.2.0
 */
public class SqlCheckRules {

    public static List<SqlCheckRuleFactory> getAllFactories(DialectType dialectType, JdbcOperations jdbc) {
        Supplier<String> schemaSupplier = new SchemaSupplier(dialectType, jdbc);
        List<SqlCheckRuleFactory> rules = new ArrayList<>();
        rules.add(new ColumnCalculationFactory());
        rules.add(new LeftFuzzyMatchFactory());
        rules.add(new NoNotNullAtInExpressionFactory());
        rules.add(new NoSpecificColumnExistsFactory());
        rules.add(new NoValidWhereClauseFactory());
        rules.add(new NoWhereClauseExistsFactory());
        rules.add(new TooManyInExpressionFactory());
        rules.add(new TooManyTableJoinFactory());
        rules.add(new TooManyOutOfLineIndexFactory());
        rules.add(new TooManyColumnRefInIndexFactory());
        rules.add(new PreferLocalOutOfLineIndexFactory());
        rules.add(new TooManyColumnDefinitionFactory());
        rules.add(new TooLongCharLengthFactory());
        rules.add(new ForeignConstraintExistsFactory());
        rules.add(new NoPrimaryKeyExistsFactory());
        rules.add(new NoTableCommentExistsFactory(schemaSupplier));
        rules.add(new TableNameInBlackListFactory());
        rules.add(new RestrictTableCharsetFactory());
        rules.add(new RestrictTableCollationFactory());
        rules.add(new RestrictPKDataTypesFactory(jdbc));
        rules.add(new RestrictPKAutoIncrementFactory(jdbc));
        rules.add(new TooManyColumnRefInPrimaryKeyFactory());
        rules.add(new RestrictIndexNamingFactory());
        rules.add(new RestrictUniqueIndexNamingFactory());
        rules.add(new NoIndexNameExistsFactory());
        rules.add(new ZeroFillExistsFactory());
        rules.add(new ColumnCharsetExistsFactory());
        rules.add(new ColumnCollationExistsFactory());
        rules.add(new RestrictColumnNotNullFactory());
        rules.add(new NoDefaultValueExistsFactory());
        rules.add(new NoColumnCommentExistsFactory(schemaSupplier));
        rules.add(new ColumnNameInBlackListFactory());
        rules.add(new RestrictColumnNameCaseFactory());
        rules.add(new RestrictTableNameCaseFactory());
        rules.add(new RestrictTableAutoIncrementFactory());
        rules.add(new SelectStarExistsFactory());
        rules.add(new MissingRequiredColumnsFactory());
        rules.add(new RestrictAutoIncrementUnsignedFactory());
        rules.add(new TooManyAlterStatementFactory());
        rules.add(new NotNullColumnWithoutDefaultValueFactory());
        rules.add(new RestrictPKNamingFactory());
        rules.add(new ProhibitedDatatypeExistsFactory());
        rules.add(new RestrictIndexDataTypesFactory(jdbc));
        rules.add(new RestrictDropObjectTypesFactory());
        rules.add(new NoPrimaryKeyNameExistsFactory());
        rules.add(new RestrictAutoIncrementDataTypesFactory());
        rules.add(new SyntaxErrorExistsFactory());
        return rules;
    }

    public static List<SqlCheckRule> getAllDefaultRules(JdbcOperations jdbc, @NonNull DialectType dialectType) {
        return SqlCheckRules.getAllFactories(dialectType, jdbc).stream()
                .map(f -> f.generate(dialectType, null)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static SqlCheckRule createByRule(JdbcOperations jdbc,
            @NonNull DialectType dialectType, @NonNull Rule rule) {
        return createByRule(getAllFactories(dialectType, jdbc), dialectType, rule);
    }

    public static SqlCheckRule createByRule(@NonNull List<SqlCheckRuleFactory> candidates,
            @NonNull DialectType dialectType, @NonNull Rule rule) {
        RuleMetadata metadata = rule.getMetadata();
        Validate.notNull(metadata, "RuleMetadata can not be null");
        RuleType ruleType = metadata.getType();
        if (ruleType != RuleType.SQL_CHECK) {
            throw new IllegalArgumentException("Can not create a sql-check rule for type, " + ruleType);
        }
        List<SqlCheckRuleType> types = Arrays.stream(SqlCheckRuleType.values())
                .filter(s -> Objects.equals("${" + s.getName() + "}", metadata.getName()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(types)) {
            throw new IllegalArgumentException(
                    "Can not find a sql check rule by name, " + metadata.getName());
        } else if (types.size() > 1) {
            throw new IllegalArgumentException(
                    "Multi sql check rules are found by name, " + metadata.getName());
        }
        SqlCheckRuleType sqlCheckRuleType = types.get(0);
        Optional<SqlCheckRuleFactory> factory = candidates.stream()
                .filter(s -> s.getSupportsType() == sqlCheckRuleType).findFirst();
        if (!factory.isPresent()) {
            throw new UnsupportedOperationException("Not support yet, " + sqlCheckRuleType.getLocalizedName());
        }
        try {
            SqlCheckRule target = factory.get().generate(dialectType, rule.getProperties());
            return target == null ? null : new SqlCheckRuleWrapper(target, rule.getAppliedDialectTypes());
        } catch (Exception e) {
            return null;
        }
    }

    static class SchemaSupplier implements Supplier<String> {

        private volatile String schema = null;
        private final DialectType dialectType;
        private final JdbcOperations jdbcOperations;

        public SchemaSupplier(DialectType dialectType, JdbcOperations jdbcOperations) {
            this.dialectType = dialectType;
            this.jdbcOperations = jdbcOperations;
        }

        @Override
        public String get() {
            if (this.schema != null) {
                return this.schema;
            } else if (this.dialectType == null || this.jdbcOperations == null) {
                return null;
            }
            this.schema = jdbcOperations.execute((ConnectionCallback<String>) con -> ConnectionPluginUtil
                    .getSessionExtension(dialectType).getCurrentSchema(con));
            return this.schema;
        }
    }

}
