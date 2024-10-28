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
package com.oceanbase.odc.service.db.model;

import java.util.List;

import com.oceanbase.odc.service.connection.database.model.UnauthorizedDBResource;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.session.model.SqlTuplesWithViolation;

import lombok.Data;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/10/18 14:11
 * @since: 4.3.3
 */
@Data
public class EditPLResp {
    // if pre-check is not successful ,sql confirmation window needs to display wrapped Sql instead of
    // the original sql.Because what really executes is wrapped Sql
    private String wrappedSql;
    // if shouldIntercepted is true，it indicates sql pre-check does not pass. sql confirmation window
    // needs to be opened
    private boolean approvalRequired;
    private List<Rule> violatedRules;
    private List<SqlTuplesWithViolation> sqls;
    private List<UnauthorizedDBResource> unauthorizedDBResources;
    // if none, the modification succeeded.If not none, the modification failed.it indicates the error
    // message
    private String errorMessage;
}
