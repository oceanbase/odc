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
package com.oceanbase.tools.dbbrowser.editor.mysql;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

/**
 * @author jingtian
 * @date 2024/1/12
 * @since ODC_release_4.2.4
 */
public class OBMySQLLessThan400ConstraintEditor extends MySQLConstraintEditor {
    @Override
    public String generateDropObjectDDL(@NotNull DBTableConstraint constraint) {
        String ddl = super.generateDropObjectDDL(constraint);
        if (constraint.getType() != DBConstraintType.PRIMARY_KEY) {
            return ddl;
        }
        return "/* Unsupported operation to drop primary key constraint */\n";
    }

    @Override
    public String generateCreateObjectDDL(DBTableConstraint constraint) {
        String ddl = super.generateCreateObjectDDL(constraint);
        if (constraint.getType() != DBConstraintType.PRIMARY_KEY) {
            return ddl;
        }
        return "/* Unsupported operation to add primary key constraint */\n";
    }
}
