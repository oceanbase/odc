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

import java.util.Optional;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author yaobin
 * @date 2024-02-01
 * @since 4.2.4
 */
@Data
public class ProcessExecutorIdentifier implements ExecutorIdentifier {

    @NotBlank
    private String ipAddress;

    private String physicalAddress;

    @NotBlank
    private String executorName;

    @NotNull
    private Long pid;


    @Override
    public String toString() {
        return Optional.ofNullable(ipAddress).orElse("") +
                append(physicalAddress) + append(executorName) + append(pid + "");
    }

    private String append(String value) {
        return ODC_EXECUTOR_FILED_DELIMITER + Optional.ofNullable(value).orElse("");
    }
}
