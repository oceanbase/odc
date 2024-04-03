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
package com.oceanbase.odc.service.audit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.metadb.audit.AuditEventEntity;
import com.oceanbase.odc.service.audit.model.AuditEventMeta;

@Component
@Profile("clientMode")
public class DesktopAuditEventHandler implements AuditEventHandler {

    private final List<AuditEventAction> supportedAuditEventActionInClientMode =
            Arrays.asList(AuditEventAction.STOP_ASYNC_TASK,
                    AuditEventAction.STOP_MOCKDATA_TASK,
                    AuditEventAction.STOP_IMPORT_TASK,
                    AuditEventAction.STOP_EXPORT_TASK,
                    AuditEventAction.STOP_EXPORT_RESULT_SET_TASK,
                    AuditEventAction.STOP_SHADOWTABLE_SYNC_TASK,
                    AuditEventAction.STOP_STRUCTURE_COMPARISON_TASK,
                    AuditEventAction.STOP_ALTER_SCHEDULE_TASK,
                    AuditEventAction.CREATE_ASYNC_TASK,
                    AuditEventAction.CREATE_MOCKDATA_TASK,
                    AuditEventAction.CREATE_IMPORT_TASK,
                    AuditEventAction.CREATE_EXPORT_TASK,
                    AuditEventAction.CREATE_EXPORT_RESULT_SET_TASK,
                    AuditEventAction.CREATE_SHADOWTABLE_SYNC_TASK,
                    AuditEventAction.CREATE_STRUCTURE_COMPARISON_TASK,
                    AuditEventAction.CREATE_ALTER_SCHEDULE_TASK,
                    AuditEventAction.ROLLBACK_TASK);
    private final List<AuditEventType> supportedAuditEventTypeInClientMode =
            Arrays.asList(AuditEventType.PERSONAL_CONFIGURATION,
                    AuditEventType.SCRIPT_MANAGEMENT, AuditEventType.DATABASE_OPERATION,
                    AuditEventType.DATASOURCE_MANAGEMENT);

    @Override
    public void handle(List<AuditEventEntity> events, HttpServletRequest servletRequest) {}

    @Override
    public List<AuditEventMeta> filterAuditEventMeta(List<AuditEventMeta> actualEventMetas) {
        return actualEventMetas.stream()
                .filter(meta -> supportedAuditEventTypeInClientMode.contains(meta.getType())
                        || supportedAuditEventActionInClientMode.contains(meta.getAction()))
                .collect(Collectors.toList());
    }

}
