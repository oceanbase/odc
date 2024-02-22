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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckResult;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link SqlCheckTaskResult}
 *
 * @author yh263208
 * @date 2022-12-15 13:19
 * @since ODC_release_4.1.0
 */
@Getter
@Setter
@ToString
public class SqlCheckTaskResult implements Serializable, FlowTaskResult {
    private boolean success;
    private Integer issueCount;
    private Integer maxLevel;
    private String error;
    private String fileName;
    private List<CheckResult> results = new ArrayList<>();
    private boolean involveTimeConsumingSql;

    public static SqlCheckTaskResult success(@NonNull List<CheckViolation> violations,
            @NonNull boolean involveTimeConsumingSql) {
        SqlCheckTaskResult result = new SqlCheckTaskResult();
        result.setSuccess(true);
        result.setIssueCount(violations.size());
        result.setResults(SqlCheckUtil.buildCheckResults(violations));
        result.setError(null);
        Optional<CheckViolation> v = violations.stream()
                .max(Comparator.comparingInt(CheckViolation::getLevel));
        result.setMaxLevel(v.isPresent() ? v.get().getLevel() : 0);
        result.setInvolveTimeConsumingSql(involveTimeConsumingSql);
        return result;
    }

    public static SqlCheckTaskResult fail(
            @NonNull Exception e) {
        SqlCheckTaskResult result = new SqlCheckTaskResult();
        result.setSuccess(false);
        result.setIssueCount(null);
        result.setError(e.getMessage());
        return result;
    }

}
