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
package com.oceanbase.odc.service.flow.instance;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;

import com.oceanbase.odc.core.flow.ExecutionConfigurer;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.NonNull;

/**
 * ODC's management and control process instance is abstract. Compared with the base class
 * {@link FlowInstance}, it has some customized code, so it is extracted into a separate class.
 *
 * @author yh263208
 * @date 2022-02-27 14:18
 * @since ODC_release_3.3.0
 */
public class OdcFlowInstance extends FlowInstance {

    /**
     * Create a new {@link FlowInstance}
     */
    public OdcFlowInstance(@NonNull String name, String description,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository, @NonNull RuntimeService runtimeService,
            @NonNull RepositoryService repositoryService) {
        super(name, description, flowableAdaptor, authenticationFacade, flowInstanceRepository,
                runtimeService, repositoryService);
    }

    public OdcFlowInstance(@NonNull String name, String description,
            Long parentFlowInstanceId,
            Long projectId,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository, @NonNull RuntimeService runtimeService,
            @NonNull RepositoryService repositoryService) {
        super(name, description, projectId, parentFlowInstanceId, flowableAdaptor,
                authenticationFacade,
                flowInstanceRepository,
                runtimeService, repositoryService);
    }

    /**
     * Load a {@link OdcFlowInstance} from {@code metaDB}
     */
    public OdcFlowInstance(@NonNull FlowInstanceEntity entity, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository, @NonNull RuntimeService runtimeService,
            @NonNull RepositoryService repositoryService) {
        super(entity, flowableAdaptor, authenticationFacade, flowInstanceRepository, runtimeService, repositoryService);
    }

    @Override
    public FlowInstanceConfigurer newFlowInstanceConfigurer() {
        return new OdcFlowInstanceConfigurer(this, processBuilder, flowableAdaptor, accessor,
                getProcessInstanceId() == null);
    }

    @Override
    protected FlowInstanceConfigurer newFlowInstanceConfigurer(@NonNull ExecutionConfigurer configurer) {
        return new OdcFlowInstanceConfigurer(this, processBuilder, configurer, flowableAdaptor, accessor,
                getProcessInstanceId() == null);
    }

}
