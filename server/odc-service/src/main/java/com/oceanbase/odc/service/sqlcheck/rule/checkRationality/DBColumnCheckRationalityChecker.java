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
package com.oceanbase.odc.service.sqlcheck.rule.checkRationality;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/6/19 01:12
 * @since: 4.3.4
 */
public class DBColumnCheckRationalityChecker implements DBObjectExistChecker, DBObjectExtractor {
    @Override
    public Boolean checkObjectExistence(DBObjectIdentity dbObjectIdentity, JdbcOperations jdbcOperations) {
        return null;
    }

    @Override
    public Boolean checkObjectNonExistence(DBObjectIdentity dbObjectIdentity, JdbcOperations jdbcOperations) {
        return null;
    }

    @Override
    public List<DBObjectIdentity> ExtractShouldExistedDBObjects(Supplier<String> schemaSupplier, Statement statement) {
        return Collections.emptyList();
    }

    @Override
    public List<DBObjectIdentity> ExtractShouldNotExistedDBObjects(Supplier<String> schemaSupplier,
            Statement statement) {
        return Collections.emptyList();
    }
}
