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

package com.oceanbase.odc.service.dlm.checker;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.connection.database.model.Database;

/**
 * @Author：tinker
 * @Date: 2023/10/30 16:15
 * @Descripition:
 */
public class DLMCheckerFactory {

    public static AbstractDLMChecker create(Database database) {
        switch (database.getDataSource().getDialectType()) {
            case OB_MYSQL:
                return new OBMySqlDLMChecker(database);
            case MYSQL:
                return new MySqlDLMChecker(database);
            default:
                throw new UnsupportedException();
        }
    }
}
