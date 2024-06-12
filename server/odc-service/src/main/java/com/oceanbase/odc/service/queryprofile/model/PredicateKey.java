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
package com.oceanbase.odc.service.queryprofile.model;

import java.util.HashMap;
import java.util.Map;

public class PredicateKey {
    private static final Map<String, String> MAP = new HashMap<>();

    static {
        // table scan
        MAP.put("access", "Access predicates");
        MAP.put("filter", "Filter predicates");
        MAP.put("range_key", "Range key");
        MAP.put("range", "Range");
        MAP.put("range_cond", "Range condition");
        MAP.put("partitions", "Scan partitions");
        // join
        MAP.put("nl_params_", "Nested loop params");
        MAP.put("conds", "Join conditions");
        MAP.put("equal_conds", "Equal conditions");
        MAP.put("other_conds", "Other conditions");
        // group
        MAP.put("group", "Group key");
        MAP.put("agg_func", "Aggregate function");
        // window function
        MAP.put("win_expr", "Window function");
        MAP.put("upper", "Window upper boundary");
        MAP.put("lower", "Window lower boundary");
        // subplan filter
        MAP.put("exec_params_", "Execution params");
        MAP.put("onetime_exprs_", "Onetime expressions");
        // distinct
        MAP.put("distinct", "Distinct columns");
        // sort
        MAP.put("sort_keys", "Sort keys");
        MAP.put("prefix_pos", "Prefix positions");
        // limit
        MAP.put("limit", "Limit");
        MAP.put("offset", "Offset");
        MAP.put("percent", "Percentage");
        // insert
        MAP.put("columns", "Columns");
        MAP.put("values", "Values");
        // delete
        MAP.put("table_columns", "Columns");
        MAP.put("update", "Update expressions");
        // merge
        MAP.put("match_conds", "Match conditions");
        MAP.put("insert_conds", "Insert conditions");
        MAP.put("update_conds", "Update conditions");
        MAP.put("delete_conds", "Delete conditions");
        // exchange
        MAP.put("pkey", "Repatition key");
    }

    public static String getLabel(String key) {
        return MAP.get(key);
    }

}
