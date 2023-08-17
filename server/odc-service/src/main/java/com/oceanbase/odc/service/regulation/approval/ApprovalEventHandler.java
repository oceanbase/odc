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
package com.oceanbase.odc.service.regulation.approval;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalFlowConfigEntity;
import com.oceanbase.odc.service.integration.IntegrationEvent;
import com.oceanbase.odc.service.integration.IntegrationEventHandler;
import com.oceanbase.odc.service.integration.model.IntegrationType;

@Component
public class ApprovalEventHandler implements IntegrationEventHandler {

    @Autowired
    private ApprovalFlowConfigService approvalFlowConfigService;

    @Override
    public boolean support(IntegrationEvent integrationEvent) {
        return IntegrationType.APPROVAL.equals(integrationEvent.getCurrentIntegrationType());
    }

    @Override
    public void preCreate(IntegrationEvent integrationEvent) {

    }

    @Override
    public void preDelete(IntegrationEvent integrationEvent) {
        externalApprovalIntegrationUsageCheck(integrationEvent.getCurrentConfig().getId(),
                AuditEventAction.DELETE_INTEGRATION);
    }

    @Override
    public void preUpdate(IntegrationEvent integrationEvent) {
        if (!integrationEvent.getCurrentConfig().getEnabled() && integrationEvent.getPreConfig().getEnabled()) {
            externalApprovalIntegrationUsageCheck(integrationEvent.getCurrentConfig().getId(),
                    AuditEventAction.DISABLE_INTEGRATION);
        }
    }

    private void externalApprovalIntegrationUsageCheck(Long integrationId, AuditEventAction auditEventAction) {
        List<ApprovalFlowConfigEntity> relatedEntities =
                approvalFlowConfigService.listRelatedFlowConfigByIntegrationId(integrationId);
        if (relatedEntities.size() > 0) {
            String names =
                    relatedEntities.stream().map(ApprovalFlowConfigEntity::getName).collect(Collectors.joining(", "));
            String errorMessage = String.format(
                    "External approval integration id=%s cannot be %s because it has been referenced to following flow config: {%s}",
                    integrationId, auditEventAction, names);
            throw new UnsupportedException(ErrorCodes.CannotOperateDueReference,
                    new Object[] {auditEventAction.getLocalizedMessage(),
                            ResourceType.ODC_EXTERNAL_APPROVAL.getLocalizedMessage(), "name", names,
                            ResourceType.ODC_APPROVAL_FLOW_CONFIG.getLocalizedMessage()},
                    errorMessage);
        }
    }
}
