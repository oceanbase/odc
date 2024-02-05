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
package com.oceanbase.odc.service.task.schedule.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.info.InfoAdapter;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;

/**
 * @author yaobin
 * @date 2024-01-09
 * @since 4.2.4
 */
public class DefaultJobImageNameProvider implements JobImageNameProvider {
    private final InfoAdapter infoAdapter;
    private final Supplier<TaskFrameworkProperties> taskFrameworkProperties;

    public DefaultJobImageNameProvider(Supplier<TaskFrameworkProperties> taskFrameworkProperties,
            InfoAdapter infoAdapter) {
        PreConditions.notNull(taskFrameworkProperties.get(), "taskFrameworkProperties");
        this.infoAdapter = infoAdapter;
        this.taskFrameworkProperties = taskFrameworkProperties;
    }

    @Override
    public String provide() {
        K8sProperties k8s = taskFrameworkProperties.get().getK8sProperties();

        List<String> candidateImages = new ArrayList<>();
        candidateImages.add(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_IMAGE_NAME));
        candidateImages.add(k8s.getPodImageName());
        candidateImages.add("odc-server:" + infoAdapter.getBuildVersion());

        return candidateImages.stream().filter(StringUtils::isNotBlank).findFirst().get();
    }
}
