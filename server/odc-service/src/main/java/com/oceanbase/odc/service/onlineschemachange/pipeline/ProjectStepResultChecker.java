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
package com.oceanbase.odc.service.onlineschemachange.pipeline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.google.common.collect.Lists;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.PrecheckResult;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.ProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.response.FullTransferStepInfoVO;
import com.oceanbase.odc.service.onlineschemachange.oms.response.FullVerifyTableStatisticVO;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectStepVO;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-12
 * @since 4.2.0
 */
@Slf4j
public class ProjectStepResultChecker {

    private final List<OmsStepName> toCheckSteps;
    private final Map<OmsStepName, ProjectStepVO> currentProjectStepMap;
    private final ProjectStepResult checkerResult;
    private Supplier<ProjectFullVerifyResultResponse> verifyResultResponseSupplier;
    private Supplier<Void> resumeProjectSupplier;
    private final ProjectProgressResponse progressResponse;

    private final boolean enableFullVerify;

    public ProjectStepResultChecker(ProjectProgressResponse progressResponse, List<ProjectStepVO> projectSteps,
            boolean enableFullVerify) {
        this.progressResponse = progressResponse;
        this.currentProjectStepMap = projectSteps.stream().collect(Collectors.toMap(ProjectStepVO::getName, a -> a));
        this.checkerResult = new ProjectStepResult();
        this.enableFullVerify = enableFullVerify;
        this.toCheckSteps = Lists.newArrayList(OmsStepName.TRANSFER_INCR_LOG_PULL,
                OmsStepName.FULL_TRANSFER, OmsStepName.INCR_TRANSFER);
        if (enableFullVerify) {
            this.toCheckSteps.add(OmsStepName.FULL_VERIFIER);
        }
    }

    public ProjectStepResultChecker withCheckerVerifyResult(
            Supplier<ProjectFullVerifyResultResponse> verifyResultSupplier) {
        this.verifyResultResponseSupplier = verifyResultSupplier;
        return this;
    }

    public ProjectStepResultChecker withResumeProject(Supplier<Void> projectResumeSupplier) {
        this.resumeProjectSupplier = projectResumeSupplier;
        return this;
    }


    public ProjectStepResult getCheckerResult() {
        checkerResult.setCurrentStep(progressResponse.getCurrentStep());
        setCurrentStepStatus();
        checkPreCheckStepResult();
        if (checkerResult.getPreCheckResult() == PrecheckResult.FAILED) {
            return checkerResult;
        }

        // todo 用户手动暂停了项目
        if (checkProjectFinished()) {
            checkerResult.setTaskStatus(TaskStatus.DONE);
        } else {
            // try to resume oms project if oms project is failed
            if (resumeProjectSupplier != null && checkStepFailed()) {
                try {
                    resumeProjectSupplier.get();
                } catch (Exception ex) {
                    log.warn("resume project error", ex);
                }
            }
            checkerResult.setTaskStatus(TaskStatus.RUNNING);
        }
        fillMigrateResult();
        checkerVerifyResult();
        evaluateTaskPercentage();
        return checkerResult;
    }

    private void setCurrentStepStatus() {
        if (progressResponse.getCurrentStep() == null) {
            return;
        }
        try {
            ProjectStepVO projectStep =
                    currentProjectStepMap.get(OmsStepName.valueOf(progressResponse.getCurrentStep()));
            if (projectStep != null) {
                checkerResult.setCurrentStepStatus(projectStep.getStatus().name());
            }
        } catch (Exception ex) {
            log.warn("Set step status occur error", ex);
        }
    }

    private void checkPreCheckStepResult() {
        ProjectStepVO precheckStep = currentProjectStepMap.get(OmsStepName.TRANSFER_PRECHECK);
        if (precheckStep.getStatus() == OmsStepStatus.FAILED) {
            checkerResult.setPreCheckResult(PrecheckResult.FAILED);
            checkerResult.setErrorMsg(precheckStep.getExtraInfo().getErrorMsg());
        }
        if (precheckStep.getProgress() != null) {
            checkerResult.setPrecheckProgressPercentage(precheckStep.getProgress());
        }
    }

    private boolean checkProjectFinished() {
        return progressResponse.getStatus() == ProjectStatusEnum.FINISHED
                || checkProjectStepFinished();
    }

    private boolean checkProjectStepFinished() {
        boolean finished = true;
        for (OmsStepName stepName : toCheckSteps) {
            if (!checkStepFinished(stepName)) {
                finished = false;
                break;
            }
        }
        return finished;
    }

    private boolean checkStepFinished(OmsStepName name) {
        OmsStepStatus status = currentProjectStepMap.get(name).getStatus();
        Integer progress = currentProjectStepMap.get(name).getProgress();
        Function<Integer, Boolean> competedFunc = (p -> p != null && p == 100);

        switch (name) {
            case INCR_TRANSFER:
                return status == OmsStepStatus.MONITORING && competedFunc.apply(progress);
            case FULL_VERIFIER:
                return enableFullVerify && status == OmsStepStatus.RUNNING && competedFunc.apply(progress);
            default:
                return status == OmsStepStatus.FINISHED && competedFunc.apply(progress);
        }
    }

    private boolean checkStepFailed() {
        boolean isProjectFailed = false;
        for (OmsStepName stepName : toCheckSteps) {
            if (currentProjectStepMap.get(stepName) != null
                    && currentProjectStepMap.get(stepName).getStatus() == OmsStepStatus.FAILED) {
                if (!enableFullVerify && stepName == OmsStepName.FULL_VERIFIER) {
                    continue;
                }
                if (currentProjectStepMap.get(stepName).getExtraInfo() != null) {
                    checkerResult.setErrorMsg(currentProjectStepMap.get(stepName).getExtraInfo().getErrorMsg());
                }
                isProjectFailed = true;
                break;
            }
        }
        return isProjectFailed;
    }

    private void checkerVerifyResult() {
        if (checkerResult.getTaskStatus() != TaskStatus.DONE) {
            return;
        }
        if (enableFullVerify) {
            ProjectFullVerifyResultResponse response = verifyResultResponseSupplier.get();
            if (response != null) {
                fillVerifyResult(response);
            }
        } else {
            checkerResult.setFullVerificationResult(FullVerificationResult.UNCHECK);
            // todo result desc
            checkerResult.setFullVerificationResultDescription("未校验");
        }
    }

    private void evaluateTaskPercentage() {

        BigDecimal result = BigDecimal.valueOf(0.1 * checkerResult.getPrecheckProgressPercentage());
        if (enableFullVerify) {
            result = result.add(BigDecimal.valueOf(0.4 * checkerResult.getFullTransferProgressPercentage()))
                    .add(BigDecimal.valueOf(0.4 * checkerResult.getFullVerificationProgressPercentage()));
        } else {
            result = result.add(BigDecimal.valueOf(0.8 * checkerResult.getFullTransferProgressPercentage()));
        }
        checkerResult.setTaskPercentage(result.doubleValue());
    }

    private void fillVerifyResult(ProjectFullVerifyResultResponse verifyResultResponse) {

        FullVerificationResult fullVerificationResult =
                getFullVerificationResult(verifyResultResponse);

        checkerResult.setFullVerificationResult(fullVerificationResult);

        List<FullVerifyTableStatisticVO> fullVerifyTableStatistics =
                verifyResultResponse.getFullVerifyTableStatistics();

        if (CollectionUtils.isNotEmpty(fullVerifyTableStatistics)) {
            String resultDesc = fullVerifyTableStatistics.stream()
                    .map(FullVerifyTableStatisticVO::getResultDesc)
                    .reduce((t, u) -> t + u).orElse("");
            checkerResult.setFullVerificationResultDescription(resultDesc);
        }

    }

    private FullVerificationResult getFullVerificationResult(ProjectFullVerifyResultResponse verifyResultResponse) {
        return verifyResultResponse.getDifferentNumber() != null && verifyResultResponse.getDifferentNumber() == 0
                ? FullVerificationResult.CONSISTENT
                : FullVerificationResult.INCONSISTENT;
    }

    private void fillMigrateResult() {
        ProjectStepVO fullTransferStep = currentProjectStepMap.get(OmsStepName.FULL_TRANSFER);
        FullTransferStepInfoVO stepInfo = (FullTransferStepInfoVO) fullTransferStep.getStepInfo();
        if (stepInfo != null) {
            checkerResult.setFullTransferEstimatedCount(stepInfo.getCapacity());
            checkerResult.setFullTransferFinishedCount(stepInfo.getProcessedRecords());
        }
        checkerResult.setFullTransferProgressPercentage(
                BigDecimal.valueOf(fullTransferStep.getProgress()).doubleValue());

        if (enableFullVerify) {
            // Set full verifier process percentage
            ProjectStepVO fullVerifyStep = currentProjectStepMap.get(OmsStepName.FULL_VERIFIER);
            checkerResult.setFullVerificationProgressPercentage(
                    BigDecimal.valueOf(fullVerifyStep.getProgress()).doubleValue());
        }

    }

    @Data
    public static class ProjectStepResult {
        private PrecheckResult preCheckResult;

        private double precheckProgressPercentage;

        private String errorMsg;

        private String currentStep;

        private String currentStepStatus;
        private TaskStatus taskStatus;

        /**
         * full transfer estimated rows count
         */
        private Long fullTransferEstimatedCount;

        /**
         * full transfer finished rows count
         */
        private Long fullTransferFinishedCount;

        /**
         * full transfer percentage
         */
        private double fullTransferProgressPercentage;

        /**
         * full verification result
         */
        private FullVerificationResult fullVerificationResult;

        /**
         * full verification result description
         */
        private String fullVerificationResultDescription;

        /**
         * full verification percentage
         */
        private double fullVerificationProgressPercentage;

        /**
         * task percentage
         */
        private double taskPercentage;

    }
}

