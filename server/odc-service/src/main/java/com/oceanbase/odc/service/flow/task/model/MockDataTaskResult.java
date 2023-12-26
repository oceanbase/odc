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

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.json.NormalDialectTypeOutput;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.schedule.MockContext;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Task execute result for {@code MockData}
 *
 * @author yh263208
 * @date 2022-03-07 11:04
 * @since ODC_release_3.3.0
 */
@Getter
@Setter
public class MockDataTaskResult implements FlowTaskResult {

    @Deprecated
    private String taskName;
    private MockTaskStatus taskStatus;
    private Long writeCount;
    private final Long conflictCount = 0L;
    private final Long ignoreCount = 0L;
    private final Long clearCount = 0L;
    private Long currentRecord;
    private Long totalGen;
    private String sessionName;
    @NormalDialectTypeOutput
    private DialectType dbMode;
    @Deprecated
    private String internalTaskId;
    @Deprecated
    private List<String> tableTaskIds;

    public MockDataTaskResult() {}

    public MockDataTaskResult(@NonNull ConnectionConfig connectionConfig, @NonNull MockContext context) {
        List<TableTaskContext> tableTaskContexts = context.getTables();
        if (CollectionUtils.isNotEmpty(tableTaskContexts)) {
            TableTaskContext tableTaskContext = tableTaskContexts.get(0);
            this.taskStatus = tableTaskContext.getStatus();
            this.writeCount = tableTaskContext.getTotalWriteCount();
            this.totalGen = tableTaskContext.getTotalGenerateCount();
        }
        this.sessionName = connectionConfig.getName();
        this.dbMode = connectionConfig.getDialectType();
    }

    public MockDataTaskResult(@NonNull ConnectionConfig connectionConfig) {
        this.sessionName = connectionConfig.getName();
        this.dbMode = connectionConfig.getDialectType();
    }

}
