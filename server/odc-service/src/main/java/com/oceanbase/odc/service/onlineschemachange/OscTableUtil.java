/*
 * Copyright (c) 2024 OceanBase.
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
package com.oceanbase.odc.service.onlineschemachange;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.db.browser.DBObjectOperators;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.extern.slf4j.Slf4j;

/**
 * util to operate osc table
 * 
 * @author longpeng.zlp
 * @date 2024/9/27 17:01
 */
@Slf4j
public class OscTableUtil {
    public static void dropNewTableIfExits(String databaseName, String tableName, ConnectionSession session) {
        List<String> list = DBSchemaAccessors.create(session)
                .showTablesLike(databaseName, tableName);
        // Drop new table suffix with _osc_new_ if exists
        if (CollectionUtils.isNotEmpty(list)) {
            DBObjectOperators.create(session)
                    .drop(DBObjectType.TABLE, databaseName, tableName);
            log.info("drop table {}.{}", databaseName, tableName);
        } else {
            log.info("table {}.{} not existed, ignore drop operation", databaseName, tableName);
        }
    }
}
