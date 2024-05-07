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
package com.oceanbase.odc.plugin.connect.obmysql.diagnose;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.OBSqlPlan;
import com.oceanbase.odc.core.shared.model.Operator;
import com.oceanbase.odc.core.shared.model.PredicateKey;
import com.oceanbase.odc.core.shared.model.QueryStatus;
import com.oceanbase.odc.core.shared.model.SqlPlanGraph;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/11
 */
@Slf4j
public class PlanGraphBuilder {
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\s+(:\\d+) => (.+)");
    private static final Pattern VALUE_GROUP_PATTERN = Pattern.compile("\\[([^]]+)]");
    private static final String EMPTY_PREDICATE = "nil";

    public static SqlPlanGraph buildPlanGraph(List<OBSqlPlan> records) {
        SqlPlanGraph graph = new SqlPlanGraph();
        Map<String, Operator> map = new HashMap<>();
        Map<String, String> parameters = new HashMap<>();
        for (OBSqlPlan record : records) {
            Operator operator = parseResult(record, parameters);
            operator.setStatus(QueryStatus.PREPARING);
            graph.insertVertex(operator);
            map.put(record.getId(), operator);
            if ("-1".equals(record.getParentId())) {
                continue;
            }
            if (!map.containsKey(record.getParentId())) {
                throw new UnexpectedException(
                        String.format("no parent node found, id=%s", record.getParentId()));
            }
            graph.insertEdge(map.get(record.getParentId()), operator, 0f);
        }
        return graph;
    }

    private static Operator parseResult(OBSqlPlan record, Map<String, String> parameters) {
        Operator operator = new Operator(record.getId(), record.getOperator());
        // set object info
        String objectName = parseObjectName(record);
        operator.setTitle(objectName);
        operator.setAttribute("Full object name", objectName);
        operator.setAttribute("Alias", record.getObjectAlias());
        // init parameters
        if (StringUtils.isNotEmpty(record.getOther()) && parameters.isEmpty()) {
            parseParameters(record.getOther(), parameters);
        }
        // parse access predicates
        if (StringUtils.isNotEmpty(record.getAccessPredicates())) {
            Map<String, List<String>> access = parsePredicates(record.getAccessPredicates(), parameters);
            operator.setAttribute("Access predicates",
                    String.join(",", access.get(PredicateKey.getLabel("access"))));
        }
        // parse filter predicates
        if (StringUtils.isNotEmpty(record.getFilterPredicates())) {
            Map<String, List<String>> filter = parsePredicates(record.getFilterPredicates(), parameters);
            operator.setAttribute("Filter predicates",
                    String.join(" AND ", filter.get(PredicateKey.getLabel("filter"))));
        }
        // parse special predicates
        if (StringUtils.isNotEmpty(record.getSpecialPredicates())) {
            Map<String, List<String>> special = parsePredicates(record.getSpecialPredicates(), parameters);
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
                        String predicateKey = PredicateKey.getLabel(keyBuilder.toString().trim());
                        if (predicateKey == null) {
                            continue;
                        }
                        String predicate = valueBuilder.toString();
                        if (predicate.startsWith("[")) {
                            LinkedList<String> values = new LinkedList<>();
                            Matcher matcher = VALUE_GROUP_PATTERN.matcher(predicate);
                            while (matcher.find()) {
                                values.add(PlanParameterSubstitutor.replace(matcher.group(1), parameters));
                            }
                            map.put(predicateKey, values);
                        } else if (!EMPTY_PREDICATE.equals(predicate)) {
                            map.put(predicateKey, Collections.singletonList(predicate));
                        }
                    } catch (Exception e) {
                        // eat exception
                    } finally {
                        keyBuilder = new StringBuilder();
                        valueBuilder = new StringBuilder();
                    }
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

    private static String parseObjectName(OBSqlPlan record) {
        String name = record.getObjectName();
        return StringUtils.isEmpty(record.getObjectOwner()) ? name
                : String.format("%s.%s", record.getObjectOwner(), name);
    }

}
