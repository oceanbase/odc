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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.PrecheckResult;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.response.FullTransferStepInfoVO;
import com.oceanbase.odc.service.onlineschemachange.oms.response.FullVerifyTableStatisticVO;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ProjectStepResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-12
 * @since 4.2.0
 */
@Slf4j
public class ProjectStepResultChecker {

    private final List<OscStepName> toCheckSteps;
    private final Map<OscStepName, OmsProjectStepVO> currentProjectStepMap;
    private final ProjectStepResult checkerResult;
    private Supplier<OmsProjectFullVerifyResultResponse> verifyResultResponseSupplier;
    private Supplier<Void> resumeProjectSupplier;
    private final OmsProjectProgressResponse progressResponse;

    private final boolean enableFullVerify;
    private final int checkProjectStepFailedTimeoutSeconds;
    private final Map<OscStepName, Long> checkFailedTimes;

    public ProjectStepResultChecker(OmsProjectProgressResponse progressResponse, List<OmsProjectStepVO> projectSteps,
            boolean enableFullVerify, int checkProjectStepFailedTimeoutSeconds,
            Map<OscStepName, Long> checkFailedTimes) {
        this.progressResponse = progressResponse;
        this.currentProjectStepMap = projectSteps.stream().collect(Collectors.toMap(OmsProjectStepVO::getName, a -> a));
        this.checkerResult = new ProjectStepResult();
        this.enableFullVerify = enableFullVerify;
        this.checkProjectStepFailedTimeoutSeconds = checkProjectStepFailedTimeoutSeconds;
        this.checkFailedTimes = checkFailedTimes;
        this.toCheckSteps = Lists.newArrayList(OscStepName.TRANSFER_INCR_LOG_PULL,
                OscStepName.FULL_TRANSFER, OscStepName.INCR_TRANSFER);
        if (enableFullVerify) {
            this.toCheckSteps.add(OscStepName.FULL_VERIFIER);
        }
    }

    public ProjectStepResultChecker withCheckerVerifyResult(
            Supplier<OmsProjectFullVerifyResultResponse> verifyResultSupplier) {
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
        if (isProjectFinished()) {
            checkerResult.setTaskStatus(TaskStatus.DONE);
        } else if (isProjectFailed() || progressResponse.getStatus().isProjectDestroyed()) {
            checkerResult.setTaskStatus(TaskStatus.FAILED);
        } else {
            checkerResult.setTaskStatus(TaskStatus.RUNNING);
        }

        fillMigrateResult();
        checkerVerifyResult();
        evaluateTaskPercentage();
        checkerResult.setIncrementCheckpoint(progressResponse.getIncrSyncCheckpoint());
        return checkerResult;
    }

    private void setCurrentStepStatus() {
        if (progressResponse.getCurrentStep() == null) {
            return;
        }
        try {
            OmsProjectStepVO projectStep =
                    currentProjectStepMap.get(OscStepName.valueOf(progressResponse.getCurrentStep()));
            if (projectStep != null) {
                checkerResult.setCurrentStepStatus(projectStep.getStatus().name());
            }
        } catch (Exception ex) {
            log.warn("Set step status occur error", ex);
        }
    }

    private void checkPreCheckStepResult() {
        OmsProjectStepVO precheckStep = currentProjectStepMap.get(OscStepName.TRANSFER_PRECHECK);
        if (precheckStep.getStatus() == OmsStepStatus.FAILED) {
            checkerResult.setPreCheckResult(PrecheckResult.FAILED);
            checkerResult.setErrorMsg(precheckStep.getExtraInfo().getErrorMsg());
        }
        if (precheckStep.getStatus() == OmsStepStatus.FINISHED) {
            checkerResult.setPreCheckResult(PrecheckResult.FINISHED);
        }
        if (precheckStep.getProgress() != null) {
            checkerResult.setPrecheckProgressPercentage(precheckStep.getProgress());
        }
    }

    private boolean isProjectFinished() {
        return progressResponse.getStatus() == OmsProjectStatusEnum.FINISHED
                || checkProjectStepFinished();
    }

    private boolean checkProjectStepFinished() {
        boolean finished = true;
        for (OscStepName stepName : toCheckSteps) {
            if (!checkStepFinished(stepName)) {
                finished = false;
                break;
            }
        }
        return finished;
    }

    @VisibleForTesting
    protected boolean checkStepFinished(OscStepName name) {
        OmsProjectStepVO omsProjectStepVO = currentProjectStepMap.get(name);
        if (null == omsProjectStepVO) {
            return true;
        }
        OmsStepStatus status = omsProjectStepVO.getStatus();
        Integer progress = omsProjectStepVO.getProgress();
        Function<Integer, Boolean> competedFunc = (p -> p != null && p == 100);
        // TODO(lx): check the time gap between local machine and remote database
        long currentSeconds = System.currentTimeMillis() / 1000;
        switch (name) {
            case INCR_TRANSFER:
                Long chkInTimestamp = progressResponse.getIncrSyncCheckpoint();
                return status == OmsStepStatus.MONITORING && competedFunc.apply(progress)
                // why set check value to 25 seconds. cause oms collect checkpoint every 10 seconds and oms writer
                // save checkpoint
                // every 10 seconds
                // max gap time is 20, we set to 25 to let it pass.
                        && (null != chkInTimestamp && (chkInTimestamp > currentSeconds
                                || Math.abs(currentSeconds - chkInTimestamp) <= 25));
            case FULL_VERIFIER:
                return enableFullVerify && status == OmsStepStatus.RUNNING && competedFunc.apply(progress);
            default:
                return status == OmsStepStatus.FINISHED && competedFunc.apply(progress);
        }
    }

    private boolean isProjectFailed() {
        boolean isProjectFailed = false;
        for (OscStepName stepName : toCheckSteps) {
            if (currentProjectStepMap.get(stepName) != null
                    && currentProjectStepMap.get(stepName).getStatus() == OmsStepStatus.FAILED) {
                if (!enableFullVerify && stepName == OscStepName.FULL_VERIFIER) {
                    continue;
                }
                if (currentProjectStepMap.get(stepName).getExtraInfo() != null) {
                    checkerResult.setErrorMsg(currentProjectStepMap.get(stepName).getExtraInfo().getErrorMsg());
                }

                // record FULL_TRANSFER failed time
                if (stepName == OscStepName.FULL_TRANSFER) {
                    long failedBeginTime = checkFailedTimes.computeIfAbsent(stepName, k -> System.currentTimeMillis());
                    long failedAccumulateTime = (System.currentTimeMillis() - failedBeginTime) / 1000;
                    if (failedAccumulateTime > checkProjectStepFailedTimeoutSeconds) {
                        log.warn("Current step failed timeout, stepName={}, "
                                + "failedAccumulateTimeSeconds={}, checkProjectStepFailedTimeoutSeconds={}",
                                stepName, failedAccumulateTime, checkProjectStepFailedTimeoutSeconds);
                        isProjectFailed = true;
                    }
                }
                break;
            } else {
                // remove if current step is not failed
                checkFailedTimes.remove(stepName);
            }
        }
        return isProjectFailed;
    }

    private void checkerVerifyResult() {
        if (checkerResult.getTaskStatus() != TaskStatus.DONE) {
            return;
        }
        if (enableFullVerify) {
            OmsProjectFullVerifyResultResponse response = verifyResultResponseSupplier.get();
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

    private void fillVerifyResult(OmsProjectFullVerifyResultResponse verifyResultResponse) {

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

    private FullVerificationResult getFullVerificationResult(OmsProjectFullVerifyResultResponse verifyResultResponse) {
        return verifyResultResponse.getDifferentNumber() != null && verifyResultResponse.getDifferentNumber() == 0
                ? FullVerificationResult.CONSISTENT
                : FullVerificationResult.INCONSISTENT;
    }

    private void fillMigrateResult() {
        OmsProjectStepVO fullTransferStep = currentProjectStepMap.get(OscStepName.FULL_TRANSFER);
        FullTransferStepInfoVO stepInfo = (FullTransferStepInfoVO) fullTransferStep.getStepInfo();
        if (stepInfo != null) {
            checkerResult.setFullTransferEstimatedCount(stepInfo.getCapacity());
            checkerResult.setFullTransferFinishedCount(stepInfo.getProcessedRecords());
        }
        checkerResult.setFullTransferProgressPercentage(
                BigDecimal.valueOf(fullTransferStep.getProgress()).doubleValue());

        if (enableFullVerify) {
            // Set full verifier process percentage
            OmsProjectStepVO fullVerifyStep = currentProjectStepMap.get(OscStepName.FULL_VERIFIER);
            if (fullVerifyStep != null && fullVerifyStep.getProgress() != null) {
                checkerResult.setFullVerificationProgressPercentage(
                        BigDecimal.valueOf(fullVerifyStep.getProgress()).doubleValue());
            }
        }
        checkerResult.setCheckFailedTime(this.checkFailedTimes);
    }

}

