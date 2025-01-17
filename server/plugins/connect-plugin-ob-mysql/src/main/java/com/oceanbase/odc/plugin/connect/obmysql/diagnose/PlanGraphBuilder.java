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

import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

import com.oceanbase.odc.common.graph.Graph;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.OBSqlPlan;
import com.oceanbase.odc.core.shared.model.Operator;
import com.oceanbase.odc.core.shared.model.QueryStatus;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraph;

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

    public static PlanGraph buildPlanGraph(List<OBSqlPlan> records) {
        Graph graph = new Graph();
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
        return PlanGraphMapper.toVO(graph);
    }

    /**
     * build by query plan in json format
     */
    public static PlanGraph buildPlanGraph(Map<String, Object> map, Map<String, String> outputFilters) {
        Graph graph = new Graph();
        parsePlanByJsonMap(map, graph, new HashMap<>(), outputFilters, "-1");
        return PlanGraphMapper.toVO(graph);
    }

    private static void parsePlanByJsonMap(Map<String, Object> jsonMap, Graph graph,
            Map<String, Operator> id2Operator, Map<String, String> outputFilter, String parentId) {
        String id = Integer.toString((int) jsonMap.get("ID"));
        Operator operator = new Operator(id, (String) jsonMap.get("OPERATOR"));
        graph.insertVertex(operator);
        id2Operator.put(operator.getGraphId(), operator);
        if (!"-1".equals(parentId)) {
            int rows = NumberUtils.isDigits(jsonMap.get("EST.ROWS").toString()) ? (int) jsonMap.get("EST.ROWS") : 0;
            graph.insertEdge(id2Operator.get(parentId), operator, rows);
        }
        operator.setStatus(QueryStatus.PREPARING);
        String name = (String) jsonMap.get("NAME");
        if (StringUtils.isNotEmpty(name)) {
            operator.setTitle(name);
            operator.setAttribute("Object name", singletonList(name));
        }
        String durationKey = jsonMap.containsKey("EST.TIME(us)") ? "EST.TIME(us)" : "COST";
        long dbTime = (int) jsonMap.get(durationKey);
        operator.setDuration(dbTime);
        operator.getOverview().put(durationKey, dbTime + "");
        Map<String, List<String>> special = parsePredicates(outputFilter.get(id), new HashMap<>());
        operator.getAttributes().putAll(special);
        jsonMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("CHILD_"))
                .forEach(entry -> parsePlanByJsonMap((Map) entry.getValue(), graph, id2Operator, outputFilter, id));
    }

    private static Operator parseResult(OBSqlPlan record, Map<String, String> parameters) {
        Operator operator = new Operator(record.getId(), record.getOperator());
        // set object info
        String objectName = parseObjectName(record);
        if (StringUtils.isNotEmpty(objectName)) {
            operator.setTitle(objectName);
            operator.setAttribute("Full object name", singletonList(objectName));
        }
        if (StringUtils.isNotEmpty(record.getObjectAlias())
                && StringUtils.equals(record.getObjectAlias(), record.getObjectName())) {
            operator.setAttribute("Alias", singletonList(record.getObjectAlias()));
        }
        // init parameters
        if (StringUtils.isNotEmpty(record.getOther()) && parameters.isEmpty()) {
            parseParameters(record.getOther(), parameters);
        }
        // parse access predicates
        if (StringUtils.isNotEmpty(record.getAccessPredicates())) {
            Map<String, List<String>> access = parsePredicates(record.getAccessPredicates(), parameters);
            if (access.containsKey("access") && Objects.nonNull(access.get("access"))) {
                operator.setAttribute("Access predicates", singletonList(String.join(",", access.get("access"))));
            }
        }
        // parse filter predicates
        if (StringUtils.isNotEmpty(record.getFilterPredicates())) {
            Map<String, List<String>> filter = parsePredicates(record.getFilterPredicates(), parameters);
            if (filter.containsKey("filter") && Objects.nonNull(filter.get("filter"))) {
                operator.setAttribute("Filter predicates", singletonList(String.join(" AND ", filter.get("filter"))));
            }
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
        if (StringUtils.isEmpty(predicates)) {
            return map;
        }
        int depth = 0;
        char[] cs = predicates.toCharArray();
        StringBuilder keyBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();
        for (char c : cs) {
            if (depth == 0) {
                if (c == '(' || c == '[') {
                    depth++;
                } else if (c == ',' || c == ' ' || c == '\n') {
                    if (keyBuilder.indexOf("=") != -1) {
                        String[] split = keyBuilder.toString().trim().split("=");
                        map.put(split[0], singletonList(split[1]));
                    }
                    keyBuilder = new StringBuilder();
                } else {
                    keyBuilder.append(c);
                }
                continue;
            }
            if (c == '\n') {
                continue;
            }
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
                if (depth == 0) {
                    try {
                        String predicateKey = keyBuilder.toString().trim();
                        String predicate = valueBuilder.toString();
                        if (predicate.isEmpty()) {
                            continue;
                        } else if (predicate.startsWith("[")) {
                            LinkedList<String> values = new LinkedList<>();
                            Matcher matcher = VALUE_GROUP_PATTERN.matcher(predicate);
                            while (matcher.find()) {
                                values.add(PlanParameterSubstitutor.replace(matcher.group(1), parameters));
                            }
                            map.put(predicateKey, values);
                        } else if (!EMPTY_PREDICATE.equals(predicate)) {
                            map.put(predicateKey, singletonList(predicate));
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
