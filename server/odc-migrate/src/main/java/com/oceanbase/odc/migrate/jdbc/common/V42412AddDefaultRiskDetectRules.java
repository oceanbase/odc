/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.migrate.jdbc.common;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.util.JdbcTemplateUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/2/27 15:38
 * @Description: []
 */
@Slf4j
@Migratable(version = "4.2.4.12", description = "add default risk level detect rules")
public class V42412AddDefaultRiskDetectRules implements JdbcMigratable {
    private static final String HIGH_RISK_DETECT_RULE_TEMPLATE =
            "{\"booleanOperator\":\"OR\",\"children\":[{\"expression\":\"ENVIRONMENT_ID\",\"operator\":\"EQUALS\",\"type\":\"CONDITION\",\"value\":\"${ENV_ID}\"},{\"expression\":\"TASK_TYPE\",\"operator\":\"IN\",\"type\":\"CONDITION\",\"value\":[\"APPLY_PROJECT_PERMISSION\",\"APPLY_DATABASE_PERMISSION\"]}],\"type\":\"CONDITION_GROUP\"}";
    private static final String DEFAULT_HIGH_RISK_LEVEL_NAME = "default high risk level detect rule";

    private TransactionTemplate transactionTemplate;
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.transactionTemplate = JdbcTemplateUtils.getTransactionTemplate(dataSource);
        transactionTemplate.execute(status -> {
            try {
                List<Long> organizationIds = listTeamOrganizationIds();
                if (organizationIds.isEmpty()) {
                    log.info("no team organization found");
                    return null;
                }

                Map<Long, Long> orgId2HighRiskLevelId = getOrgId2HighRiskLevelIdMap(organizationIds);
                if (orgId2HighRiskLevelId.isEmpty()) {
                    log.warn("no high risk level found for organization {}", organizationIds);
                    return null;
                }

                Map<Long, Long> orgId2ProdEnvId = getOrgId2ProdEnvIdMap(organizationIds);
                if (organizationIds.isEmpty()) {
                    log.warn("no prod env found for organization {}", organizationIds);
                    return null;
                }

                batchInsertRules(generateRulesToInsert(orgId2HighRiskLevelId, orgId2ProdEnvId));
            } catch (Exception e) {
                log.error("add default risk detect rules failed", e);
                status.setRollbackOnly();
                throw new RuntimeException("add default risk detect rules failed", e);
            }
            return null;
        });
    }

    private List<Long> listTeamOrganizationIds() {
        String queryTeamOrganization = "select id from iam_organization where type = 'TEAM';";
        return jdbcTemplate.queryForList(queryTeamOrganization, Long.class);
    }

    private Map<Long, Long> getOrgId2HighRiskLevelIdMap(List<Long> organizationIds) {
        String sql =
                "select id, organization_id from regulation_risklevel where level = 3 and organization_id in (:organizationIds)";
        Map<String, List<Long>> params = Collections.singletonMap("organizationIds", organizationIds);
        return namedParameterJdbcTemplate.query(sql, params, rs -> {
            Map<Long, Long> resultMap = new HashMap<>();
            while (rs.next()) {
                resultMap.put(rs.getLong("organization_id"), rs.getLong("id"));
            }
            return resultMap;
        });
    }

    private Map<Long, Long> getOrgId2ProdEnvIdMap(List<Long> organizationIds) {
        String sql =
                "select id, organization_id from collaboration_environment where name = '${com.oceanbase.odc.builtin-resource.collaboration.environment.prod.name}' and organization_id in (:organizationIds)";
        Map<String, List<Long>> params = Collections.singletonMap("organizationIds", organizationIds);
        return namedParameterJdbcTemplate.query(sql, params, rs -> {
                Map<Long, Long> resultMap = new HashMap<>();
                while (rs.next()) {
                    resultMap.put(rs.getLong("organization_id"), rs.getLong("id"));
                }
                return resultMap;
            });
    }

    private List<InnerRiskDetectRule> generateRulesToInsert(Map<Long, Long> orgId2HighRiskLevelId,
        Map<Long, Long> orgId2ProdEnvId) {
        List<InnerRiskDetectRule> rules = new ArrayList<>();
        Set<Long> organizationIds = orgId2HighRiskLevelId.keySet();
        for (Long orgId : organizationIds) {
            Long highRiskLevelId = orgId2HighRiskLevelId.get(orgId);
            Long prodEnvId = orgId2ProdEnvId.get(orgId);
            if (prodEnvId == null) {
                log.warn("prod env not found for organization {}", orgId);
                continue;
            }
            InnerRiskDetectRule rule =
                new InnerRiskDetectRule(highRiskLevelId, orgId,
                    HIGH_RISK_DETECT_RULE_TEMPLATE.replace("${ENV_ID}", String.valueOf(prodEnvId)));
            rules.add(rule);
        }
        return rules;
    }

    private void batchInsertRules(List<InnerRiskDetectRule> rules) {
        log.info("start to batch insert default risk detect rules");
        String sql =
            "INSERT INTO regulation_riskdetect_rule (create_time, update_time, name, risk_level_id, is_builtin, creator_id, organization_id, value_json) values (now(), now(), ?, ?, 1, ?, ?, ?) on duplicate key update id = id";
        int[] affectedRows = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                InnerRiskDetectRule rule = rules.get(i);
                ps.setString(1, DEFAULT_HIGH_RISK_LEVEL_NAME);
                ps.setLong(2, rule.getRiskLevelId());
                ps.setLong(3, 1L);
                ps.setLong(4, rule.getOrganizationId());
                ps.setString(5, rule.getValueJson());
            }
            @Override
            public int getBatchSize() {
                return rules.size();
            }
        });
        log.info("batch insert default risk detect rules finished, affected rows: {}",
            Arrays.stream(affectedRows).sum());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class InnerRiskDetectRule {
        private Long riskLevelId;
        private Long organizationId;
        private String valueJson;
    }
}
