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
package com.oceanbase.odc.service.shadowtable;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.shadowtable.ShadowTableComparingTaskEntity;
import com.oceanbase.odc.metadb.shadowtable.TableComparingRepository;
import com.oceanbase.odc.metadb.shadowtable.TableComparingTaskRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskParameter;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskResult;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncResp.TableComparing;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2022/9/22 下午7:12
 * @Description: []
 */
@Service
@SkipAuthorize("permission check inside getForConnect")
public class ShadowTableSyncService {
    @Autowired
    private TableComparingTaskRepository comparingTaskRepository;

    @Autowired
    private TableComparingRepository comparingRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    @Qualifier("shadowTableSyncTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    public ShadowTableSyncTaskContext create(@NonNull ShadowTableSyncTaskParameter taskParameter,
            @NonNull Long taskId) {
        ShadowTableComparingTaskEntity taskEntity =
                comparingTaskRepository.findByIdAndCreatorId(taskParameter.getComparingTaskId(),
                        currentUserId()).orElseThrow(
                                () -> new NotFoundException(
                                        ResourceType.ODC_SHADOWTABLE_COMPARING_TASK, "comparingTaskId",
                                        taskParameter.getComparingTaskId()));
        List<TableComparing> tables =
                comparingRepository.findByComparingTaskId(taskEntity.getId()).stream()
                        .map(TableComparing::toTableComparing)
                        .filter(comparing -> comparing.getComparingResult().needsSync())
                        .collect(Collectors.toList());

        ConnectionConfig connectionConfig = taskParameter.getConnectionConfig();
        taskParameter.setConnectionConfig(connectionConfig);
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connectionConfig);
        ConnectionSession connectionSession = factory.generateSession();
        ShadowTableSyncTask task =
                new ShadowTableSyncTask(taskParameter.getComparingTaskId(), taskParameter.getErrorStrategy(), tables,
                        connectionSession, taskId, currentUserId());
        Future<ShadowTableSyncTaskResult> future = executor.submit(task);
        ShadowTableSyncTaskContext context = new ShadowTableSyncTaskContext(task, future);
        return context;
    }


    private Long currentUserId() {
        return authenticationFacade.currentUserId();
    }
}
