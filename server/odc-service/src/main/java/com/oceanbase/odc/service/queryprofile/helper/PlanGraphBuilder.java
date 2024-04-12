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
package com.oceanbase.odc.service.queryprofile.helper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.queryprofile.model.Operator;
import com.oceanbase.odc.service.queryprofile.model.PredicateKey;
import com.oceanbase.odc.service.queryprofile.model.SqlPlanGraph;
import com.oceanbase.odc.service.queryprofile.model.SqlProfile.Status;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/11
 */
@Slf4j
public class PlanGraphBuilder {
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("^\\s+(:\\d+) => ('.+')&");
    private static final Pattern VALUE_GROUP_PATTERN = Pattern.compile("\\[([^]]+)]");
    private static final String EMPTY_PREDICATE = "nil";

    public static SqlPlanGraph getOBPlanGraph(Statement statement, String planId, DialectType dialectType)
            throws SQLException {
        SqlPlanGraph graph = new SqlPlanGraph();
        SqlBuilder sqlBuilder = dialectType.isMysql() ? new MySQLSqlBuilder() : new OracleSqlBuilder();
        sqlBuilder.append("select id, parent_id, operator, object_owner, object_name, object_alias, ")
                .append("other, access_predicates, filter_predicates, special_predicates from ")
                .append(dialectType.isMysql() ? "oceanbase" : "sys")
                .append(".v$ob_sql_plan where plan_id=")
                .append(planId)
                .append(" order by id asc");
        try (ResultSet rs = statement.executeQuery(sqlBuilder.toString())) {
            Map<String, Operator> map = new HashMap<>();
            Map<String, String> parameters = new HashMap<>();
            while (rs.next()) {
                Operator operator = parseResult(rs, parameters);
                operator.setStatus(Status.PREPARING);
                graph.insertVertex(operator);
                map.put(rs.getString("id"), operator);
                if ("-1".equals(rs.getString("parent_id"))) {
                    continue;
                }
                if (!map.containsKey(rs.getString("parent_id"))) {
                    throw new UnexpectedException(
                            String.format("no parent node found, id=%s, plan_id=%s", rs.getString("id"), planId));
                }
                graph.insertEdge(map.get(rs.getString("parent_id")), operator, 0f);
            }
        }
        return graph;
    }

    private static Operator parseResult(ResultSet rs, Map<String, String> parameters)
            throws SQLException {
        Operator operator = new Operator(rs.getString("id"), rs.getString("operator"));
        // set object info
        operator.setAttribute("Full object name", parseObjectName(rs));
        operator.setAttribute("Alias", rs.getString("object_alias"));
        // init parameters
        if (StringUtils.isNotEmpty(rs.getString("other")) && parameters.isEmpty()) {
            parseParameters(rs.getString("other"), parameters);
        }
        // parse access predicates
        if (rs.getString("access_predicates") != null) {
            Map<String, List<String>> access = parsePredicates(rs.getString("access_predicates"), parameters);
            operator.setAttribute("Access predicates", String.join(",", access.get("access")));
        }
        // parse filter predicates
        if (rs.getString("filter_predicates") != null) {
            Map<String, List<String>> filter = parsePredicates(rs.getString("filter_predicates"), parameters);
            operator.setAttribute("Filter predicates", String.join(" AND ", filter.get("filter")));
        }
        // parse special predicates
        if (rs.getString("special_predicates") != null) {
            Map<String, List<String>> special = parsePredicates(rs.getString("special_predicates"), parameters);
            operator.getAttributes().putAll(special);
        }

        return operator;
    }

    public static Map<String, List<String>> parsePredicates(String predicates, Map<String, String> parameters) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        int depth = 0;
        char[] cs = predicates.toCharArray();
        StringBuilder keyBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();
        for (char c : cs) {
            if (depth == 0) {
                if (c == '(' || c == '[') {
                    depth++;
                } else if (c == ',') {
                    keyBuilder = new StringBuilder();
                } else {
                    keyBuilder.append(c);
                }
                continue;
            }
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
                if (depth == 0) {
                    try {
                        String predicateKey = keyBuilder.toString().trim();
                        PredicateKey key = PredicateKey.valueOf(predicateKey);
                        String predicate = valueBuilder.toString();
                        if (predicate.startsWith("[")) {
                            LinkedList<String> values = new LinkedList<>();
                            Matcher matcher = VALUE_GROUP_PATTERN.matcher(predicate);
                            while (matcher.find()) {
                                values.add(CustomStringSubstitutor.replace(matcher.group(1), parameters));
                            }
                            map.put(key.getDisplayName(), values);
                        } else if (!EMPTY_PREDICATE.equals(predicate)) {
                            map.put(key.getDisplayName(), Collections.singletonList(predicate));
                        }
                    } catch (Exception e) {
                        // eat exception
                    }
                    keyBuilder = new StringBuilder();
                    valueBuilder = new StringBuilder();
                    continue;
                }
            }
            valueBuilder.append(c);
        }
        return map;
    }

    private static void parseParameters(String other, Map<String, String> parameters) {
        String[] lines = other.split("\n");
        for (String line : lines) {
            Matcher matcher = PARAMETER_PATTERN.matcher(line);
            if (matcher.matches()) {
                parameters.put(matcher.group(1), matcher.group(2));
            }
        }
    }

    private static String parseObjectName(ResultSet rs) throws SQLException {
        String fullObjectName = rs.getString("object_name");
        if (rs.getString("object_owner") != null) {
            fullObjectName = rs.getString("object_owner") + "." + fullObjectName;
        }
        return fullObjectName;
    }

}
