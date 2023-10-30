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
package com.oceanbase.odc.service.session.interceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.TraceStage;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.SqlConsoleRuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.SqlConsoleRules;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.model.SqlTuplesWithViolation;
import com.oceanbase.tools.dbbrowser.parser.ParserUtil;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/7/17 15:37
 * @Description: []
 */
@Slf4j
@Component
public class SqlConsoleInterceptor implements SqlExecuteInterceptor {

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private SqlConsoleRuleService sqlConsoleRuleService;

    @Override
    public boolean preHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull Map<String, Object> context) throws IOException {
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        if (Objects.isNull(ruleSetId) || isIndividualTeam()) {
            return true;
        }

        Optional<Integer> queryLimit = sqlConsoleRuleService.getProperties(ruleSetId, SqlConsoleRules.MAX_RETURN_ROWS,
                session.getDialectType(), Integer.class);
        queryLimit.ifPresent(limit -> {
            if (Objects.isNull(request.getQueryLimit()) || request.getQueryLimit() > limit) {
                request.setQueryLimit(limit);
            }
        });
        AtomicBoolean allowExecute = new AtomicBoolean(true);

        List<SqlTuple> sqlTuples = response.getSqls().stream().map(SqlTuplesWithViolation::getSqlTuple).collect(
                Collectors.toList());
        Optional<Integer> maxSqls = sqlConsoleRuleService.getProperties(ruleSetId, SqlConsoleRules.MAX_EXECUTE_SQLS,
                session.getDialectType(), Integer.class);
        if (maxSqls.isPresent()) {
            if (sqlTuples.size() > maxSqls.get()) {
                ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.MAX_EXECUTE_SQLS.getRuleName())
                        .ifPresent(rule -> response.getViolatedRules().add(rule));
                allowExecute.set(false);
            }
        }
        Map<String, BasicResult> sqlId2BasicResult = new HashMap<>();
        sqlTuples.forEach(sql -> sqlId2BasicResult.putIfAbsent(sql.getSqlId(),
                determineSqlType(sql.getOriginalSql(), session.getDialectType())));

        boolean forbiddenToCreatePL =
                sqlConsoleRuleService.isForbidden(SqlConsoleRules.NOT_ALLOWED_CREATE_PL, session);
        Optional<List<String>> allowSqlTypesOpt = sqlConsoleRuleService.getListProperties(ruleSetId,
                SqlConsoleRules.ALLOW_SQL_TYPES, session.getDialectType(), String.class);

        for (SqlTuplesWithViolation violation : response.getSqls()) {
            try (TraceStage stage = violation.getSqlTuple().getSqlWatch().start(SqlExecuteStages.SQL_CONSOLE_RULE)) {
                List<Rule> violatedRules = violation.getViolatedRules();
                BasicResult parseResult = sqlId2BasicResult.get(violation.getSqlTuple().getSqlId());
                if (parseResult.isPlDdl() && forbiddenToCreatePL) {
                    ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.NOT_ALLOWED_CREATE_PL.getRuleName())
                            .ifPresent(violatedRules::add);
                    allowExecute.set(false);
                }
                if (allowSqlTypesOpt.isPresent()) {
                    /**
                     * skip syntax error
                     */
                    if (Objects.nonNull(parseResult.getSyntaxError()) && parseResult.getSyntaxError()) {
                        continue;
                    }
                    if (!allowSqlTypesOpt.get().contains(parseResult.getSqlType().name())) {
                        ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.ALLOW_SQL_TYPES.getRuleName())
                                .ifPresent(violatedRules::add);
                        allowExecute.set(false);
                    }
                }
            }
        }
        return allowExecute.get();
    }

    @Override
    public void afterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull Map<String, Object> context) throws Exception {
        try (TraceStage stage = response.getTraceWatch().start(SqlExecuteStages.SQL_CONSOLE_RULE)) {
            if (response.getStatus() != SqlExecuteStatus.SUCCESS) {
                return;
            }
            if (isIndividualTeam()) {
                response.setAllowExport(true);
                return;
            }
            Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
            if (Objects.isNull(ruleSetId) || isIndividualTeam()) {
                return;
            }
            if (sqlConsoleRuleService.isForbidden(SqlConsoleRules.NOT_ALLOWED_EDIT_RESULTSET, session)) {
                if (Objects.nonNull(response.getResultSetMetaData())) {
                    response.getResultSetMetaData().setEditable(false);
                }
            }
            response.setAllowExport(
                    !sqlConsoleRuleService.isForbidden(SqlConsoleRules.NOT_ALLOWED_EXPORT_RESULTSET, session));
        }
    }

    private boolean isIndividualTeam() {
        return authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL;
    }

    private BasicResult determineSqlType(@NonNull String sql, @NonNull DialectType dialectType) {
        BasicResult basicResult = new BasicResult(SqlType.OTHERS);
        if (dialectType.isMysql()) {
            basicResult = ParserUtil.parseMysqlType(sql);
        } else if (dialectType.isOracle()) {
            basicResult = ParserUtil.parseOracleType(sql);
        }
        if (Objects.nonNull(basicResult.getSqlType()) && basicResult.getSqlType() == SqlType.UNKNOWN) {
            basicResult.setSqlType(SqlType.OTHERS);
        }
        return basicResult;
    }

    @Override
    public int getOrder() {
        return 2;
    }

}
