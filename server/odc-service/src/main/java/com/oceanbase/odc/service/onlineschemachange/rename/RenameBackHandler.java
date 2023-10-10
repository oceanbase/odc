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

package com.oceanbase.odc.service.onlineschemachange.rename;

import java.util.List;

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-23
 * @since 4.2.0
 */
@Slf4j
public class RenameBackHandler {

    private final RenameTableHandler renameTableHandler;

    public RenameBackHandler(RenameTableHandler renameTableHandler) {
        this.renameTableHandler = renameTableHandler;
    }

    public void renameBack(ConnectionSession connectionSession,
            OnlineSchemaChangeScheduleTaskParameters taskParameters) {

        /*
         * If the original table was successfully renamed to _old but the second rename operation failed,
         * rollback the first renaming
         */
        try {
            DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
            List<String> renamedTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                    taskParameters.getRenamedTableNameUnwrapped());

            List<String> originTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                    taskParameters.getOriginTableNameUnwrapped());

            if (!CollectionUtils.isEmpty(renamedTable) && CollectionUtils.isEmpty(originTable)) {
                renameTableHandler.rename(taskParameters.getDatabaseName(),
                        taskParameters.getRenamedTableName(), taskParameters.getOriginTableName());
                log.info("Because previous swap occur error, so we rename back, renamed {} TO {}",
                        taskParameters.getRenamedTableNameWithSchema(), taskParameters.getOriginTableNameWithSchema());
            }
        } catch (Exception exception) {
            log.warn("Rename back occur error", exception);
        }
    }
}
