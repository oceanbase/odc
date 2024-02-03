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
package com.oceanbase.odc.service.task.caller;

import static com.oceanbase.odc.service.task.constants.JobConstants.ODC_EXECUTOR_FILED_DELIMITER;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.task.enums.TaskRunMode;

/**
 * @author yaobin
 * @date 2024-02-01
 * @since 4.2.4
 */
public class ExecutorIdentifierParser {

    public static ExecutorIdentifier parser(TaskRunMode runMode, String executorIdentifierString) {
        if (runMode == TaskRunMode.K8S) {
            return parseK8sExecutorIdentifier(executorIdentifierString);
        }
        return parseProcessExecutorIdentifier(executorIdentifierString);
    }

    private static K8sExecutorIdentifier parseK8sExecutorIdentifier(String executorIdentifierString) {
        String[] fieldValues = executorIdentifierString.split(ODC_EXECUTOR_FILED_DELIMITER);
        replaceBlankToNull(fieldValues);
        K8sExecutorIdentifier kei = new K8sExecutorIdentifier();
        kei.setCloudProvider(fieldValues[0]);
        kei.setRegion(fieldValues[1]);
        kei.setClusterName(fieldValues[2]);
        kei.setNamespace(fieldValues[3]);
        kei.setExecutorName(fieldValues[4]);
        kei.setPodIdentity(fieldValues[5]);
        return kei;
    }

    private static ProcessExecutorIdentifier parseProcessExecutorIdentifier(String executorIdentifierString) {
        String[] fieldValues = executorIdentifierString.split(ODC_EXECUTOR_FILED_DELIMITER);
        replaceBlankToNull(fieldValues);
        ProcessExecutorIdentifier pei = new ProcessExecutorIdentifier();
        pei.setIpv4Address(fieldValues[0]);
        pei.setPhysicalAddress(fieldValues[1]);
        pei.setExecutorName(fieldValues[2]);
        PreConditions.notNull(fieldValues[3], "pid");
        pei.setPid(Long.parseLong(fieldValues[3]));
        return pei;
    }


    private static void replaceBlankToNull(String[] fieldValues) {
        for (int i = 0; i < fieldValues.length; i++) {
            if (StringUtils.isBlank(fieldValues[i])) {
                fieldValues[i] = null;
            }
        }
    }

}
