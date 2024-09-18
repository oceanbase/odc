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
package com.oceanbase.odc.service.common.response.batch;

import java.util.Collection;
import java.util.List;

import lombok.Data;

/**
 * @author keyang
 * @date 2024/09/18
 * @since 4.3.2
 */
@Data
public class BatchOperationResponseData {
    private int total;
    private int successCount;
    private int failedCount;
    private List<BatchOperationResponseDetail> details;

    public BatchOperationResponseData() {
        this.total = 0;
        this.successCount = 0;
        this.failedCount = 0;
    }

    public void addSuccess(Collection<BatchOperationResponseDetail> details) {
        this.successCount += details.size();
        this.total += details.size();
        this.details.addAll(details);
    }

    public void addFailed(Collection<BatchOperationResponseDetail> details) {
        this.failedCount += details.size();
        this.total += details.size();
        this.details.addAll(details);
    }
}
