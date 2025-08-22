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
package com.oceanbase.odc.service.ai.knowledgebase.listener;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.ai.knowledgebase.dbschema.SchemaKBBuildManager;
import com.oceanbase.odc.service.connection.database.model.DatabaseTransferEvent;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/31
 */
@Component
public class DatabaseTransferEventListener {

    @Autowired(required = false)
    private SchemaKBBuildManager schemaKBBuildManager;

    @EventListener(DatabaseTransferEvent.class)
    public void onEvent(DatabaseTransferEvent event) {
        if (schemaKBBuildManager == null) {
            return;
        }
        List<Long> dbIds = (List<Long>) event.getSource();
        schemaKBBuildManager.submitBuildSchemaKBTaskForDatabaseIds(dbIds, null, null);
    }

}
