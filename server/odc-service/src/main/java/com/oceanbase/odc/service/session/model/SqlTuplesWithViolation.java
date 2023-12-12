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
package com.oceanbase.odc.service.session.model;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/7/26 20:24
 * @Description: []
 */
@Data
public class SqlTuplesWithViolation {

    private SqlTuple sqlTuple;
    private List<Rule> violatedRules;

    public SqlTuplesWithViolation(SqlTuple sqls, List<Rule> violatedRules) {
        PreConditions.notNull(sqls, "sqls");
        PreConditions.notNull(violatedRules, "violatedRules");
        this.sqlTuple = sqls;
        this.violatedRules = violatedRules;
    }

    public static SqlTuplesWithViolation newSqlTuplesWithViolation(SqlTuple sqls) {
        PreConditions.notNull(sqls, "sqls");
        return new SqlTuplesWithViolation(sqls, new ArrayList<>());
    }
}
