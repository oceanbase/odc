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

package com.oceanbase.odc.service.flow.task;

import java.util.Optional;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.RetryExecutor;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.flow.BaseFlowableDelegate;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.client.ApprovalClient;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/9/1 14:00
 */
@Slf4j
public class CreateExternalApprovalTask extends BaseFlowableDelegate {

    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private ApprovalClient approvalClient;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private RiskLevelService riskLevelService;

    private final RetryExecutor retryExecutor = RetryExecutor.builder().retryIntervalMillis(1000).retryTimes(3).build();

    @Override
    protected void run(DelegateExecution execution) throws Exception {
        FlowApprovalInstance flowApprovalInstance;
        try {
            flowApprovalInstance = getFlowApprovalInstance(execution);
        } catch (Exception e) {
            log.warn(
                    "Get flow approval instance failed, the flow instance is coming to an end, activityId={}, processDefinitionId={}",
                    execution.getCurrentActivityId(), execution.getProcessDefinitionId(), e);
            try {
                flowInstanceRepository.updateStatusById(FlowTaskUtil.getFlowInstanceId(execution),
                        FlowStatus.EXECUTION_FAILED);
            } finally {
                execution.setVariable(RuntimeTaskConstants.SUCCESS_CREATE_EXT_INS, false);
            }
            return;
        }
        Long externalApprovalId = flowApprovalInstance.getExternalApprovalId();
        try {
            Verify.notNull(externalApprovalId, "externalApprovalId");
            IntegrationConfig config = integrationService.detailWithoutPermissionCheck(externalApprovalId);
            ApprovalProperties properties = ApprovalProperties.from(config);
            TemplateVariables variables = FlowTaskUtil.getTemplateVariables(execution.getVariables());
            // add riskLevel to variables
            Optional<RiskLevel> riskLevelOpt =
                    riskLevelService.findRawById(Long.valueOf(FlowTaskUtil.getRiskLevel(execution)));
            if (riskLevelOpt.isPresent()) {
                String riskLevelNameKey = riskLevelOpt.get().getName();
                if (StringUtils.isTranslatable(riskLevelNameKey)) {
                    String riskLevelName = I18n.translate(StringUtils.getTranslatableKey(riskLevelNameKey), null,
                            LocaleContextHolder.getLocale());
                    variables.setAttribute(Variable.RISK_LEVEL, riskLevelName);
                }
            }
            String externalFlowInstanceId = approvalClient.start(properties, variables);
            flowApprovalInstance.setExternalFlowInstanceId(externalFlowInstanceId);
            flowApprovalInstance.update();
            execution.setVariable(RuntimeTaskConstants.SUCCESS_CREATE_EXT_INS, true);
        } catch (Exception e) {
            log.warn("Create external approval instance failed, the flow instance is coming to an end, "
                    + "flowApprovalInstanceId={}, externalApprovalId={}",
                    flowApprovalInstance.getId(), externalApprovalId, e);
            try {
                flowApprovalInstance.setStatus(FlowNodeStatus.FAILED);
                flowApprovalInstance.setComment(e.getLocalizedMessage());
                flowApprovalInstance.update();
                flowInstanceRepository.updateStatusById(flowApprovalInstance.getFlowInstanceId(),
                        FlowStatus.EXECUTION_FAILED);
            } finally {
                execution.setVariable(RuntimeTaskConstants.SUCCESS_CREATE_EXT_INS, false);
            }
        }
    }

    private FlowApprovalInstance getFlowApprovalInstance(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String processDefinitionId = execution.getProcessDefinitionId();
        Optional<Optional<Long>> flowInstanceIdOpt = retryExecutor.run(
                () -> flowableAdaptor.getFlowInstanceIdByProcessDefinitionId(processDefinitionId), Optional::isPresent);
        if (!flowInstanceIdOpt.isPresent() || !flowInstanceIdOpt.get().isPresent()) {
            log.warn("Flow instance id does not exist, processDefinitionId={}", processDefinitionId);
            throw new IllegalStateException(
                    "Can not find flow instance id by process definition id " + processDefinitionId);
        }
        Long flowInstanceId = flowInstanceIdOpt.get().get();
        Optional<FlowApprovalInstance> instanceOpt =
                flowableAdaptor.getApprovalInstanceByActivityId(activityId, flowInstanceId);
        if (!instanceOpt.isPresent()) {
            log.warn("Flow approval instance does not exist, activityId={}, flowInstanceId={}", activityId,
                    flowInstanceId);
            throw new IllegalStateException("Can not find flow approval instance by activityId " + activityId);
        }
        return instanceOpt.get();
    }

}
