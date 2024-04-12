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

public enum PredicateKey {
    // table scan
    access("Access predicates"),
    filter("Filter predicates"),
    range_key("Range key"),
    range("Range"),
    range_cond("Range condition"),
    partitions("Scan partitions"),
    // join
    nl_params_("Nested loop params"),
    conds("Join conditions"),
    equal_conds("Equal conditions"),
    other_conds("Other conditions"),
    // group
    group("Group key"),
    agg_func("Aggregate function"),
    // window function
    win_expr("Window function"),
    upper("Window upper boundary"),
    lower("Window lower boundary"),
    // subplan filter
    exec_params_("Execution params"),
    onetime_exprs_("Onetime expressions"),
    // distinct
    distinct("Distinct columns"),
    // sort
    sort_keys("Sort keys"),
    prefix_pos("Prefix positions"),
    // limit
    limit("Limit"),
    offset("Offset"),
    percent("Percentage"),
    // for update
    // insert
    columns("Columns"),
    values("Values"),
    // delete
    table_columns("Columns"),
    update("Update expressions"),
    // merge
    match_conds("Match conditions"),
    insert_conds("Insert conditions"),
    update_conds("Update conditions"),
    delete_conds("Delete conditions"),
    // exchange
    pkey("Repatition key"),
    ;

    private final String displayName;

    PredicateKey(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
