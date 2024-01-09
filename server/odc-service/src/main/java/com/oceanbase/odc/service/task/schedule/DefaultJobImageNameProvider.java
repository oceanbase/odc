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
package com.oceanbase.odc.service.task.schedule;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.info.InfoAdapter;
<<<<<<< HEAD
<<<<<<< HEAD
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
=======
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties.K8sProperties;
>>>>>>> 51d5e485d (feat(taskframework): add pod config in meta-db and task legacy run model (#1367))
=======
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
>>>>>>> 848537093 (add config)
import com.oceanbase.odc.service.task.constants.JobEnvConstants;

/**
 * @author yaobin
 * @date 2024-01-09
 * @since 4.2.4
 */
public class DefaultJobImageNameProvider implements JobImageNameProvider {
    private final InfoAdapter infoAdapter;
    private final TaskFrameworkProperties taskFrameworkProperties;

    public DefaultJobImageNameProvider(InfoAdapter infoAdapter, TaskFrameworkProperties taskFrameworkProperties) {
        this.infoAdapter = infoAdapter;
        this.taskFrameworkProperties = taskFrameworkProperties;
    }

    @Override
    public String provide() {
        K8sProperties k8s = taskFrameworkProperties.getK8s();

        List<String> candidateImages = new ArrayList<>();
<<<<<<< HEAD
<<<<<<< HEAD
        candidateImages.add(SystemUtils.getEnvOrProperty(JobEnvConstants.ODC_IMAGE_NAME));
        candidateImages.add(k8s.getPodImageName());
=======
        candidateImages.add(k8s.getPodImageName());
        candidateImages.add(SystemUtils.getEnvOrProperty(JobEnvConstants.ODC_IMAGE_NAME));
>>>>>>> 51d5e485d (feat(taskframework): add pod config in meta-db and task legacy run model (#1367))
=======
        candidateImages.add(SystemUtils.getEnvOrProperty(JobEnvConstants.ODC_IMAGE_NAME));
        candidateImages.add(k8s.getPodImageName());
>>>>>>> 27f7e016d (candidateImages first get from env)
        candidateImages.add("odc-server:" + infoAdapter.getBuildVersion());

        return candidateImages.stream().filter(StringUtils::isNotBlank).findFirst().get();
    }
}
