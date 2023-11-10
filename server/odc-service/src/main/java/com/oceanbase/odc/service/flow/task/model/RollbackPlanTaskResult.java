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
package com.oceanbase.odc.service.flow.task.model;

import java.io.Serializable;

import com.oceanbase.odc.core.flow.model.FlowTaskResult;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link RollbackPlanTaskResult}
 *
 * @author jingtian
 * @date 2023/5/18
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@ToString
public class RollbackPlanTaskResult implements Serializable, FlowTaskResult {
    private boolean success;
    private boolean generated;
    private String objectId;
    private String error;

    public static RollbackPlanTaskResult success(@NonNull String objectId) {
        RollbackPlanTaskResult result = new RollbackPlanTaskResult();
        result.setSuccess(true);
        result.setError(null);
        result.setObjectId(objectId);
        result.setGenerated(true);
        return result;
    }

    public static RollbackPlanTaskResult skip() {
        RollbackPlanTaskResult result = new RollbackPlanTaskResult();
        result.setSuccess(true);
        result.setError(null);
        result.setObjectId(null);
        result.setGenerated(false);
        return result;
    }

    public static RollbackPlanTaskResult fail(@NonNull String message) {
        RollbackPlanTaskResult result = new RollbackPlanTaskResult();
        result.setSuccess(false);
        result.setError(message);
        result.setObjectId(null);
        result.setGenerated(false);
        return result;
    }
}
