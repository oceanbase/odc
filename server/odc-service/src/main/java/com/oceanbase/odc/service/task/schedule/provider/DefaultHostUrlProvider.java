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

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2023-12-01
 * @since 4.2.4
 */
public class DefaultHostUrlProvider implements HostUrlProvider {

    private final HostProperties configProperties;
    private final Supplier<TaskFrameworkProperties> taskFrameworkProperties;

    public DefaultHostUrlProvider(Supplier<TaskFrameworkProperties> taskFrameworkProperties,
            HostProperties configProperties) {
        PreConditions.notNull(taskFrameworkProperties.get(), "taskFrameworkProperties");
        this.taskFrameworkProperties = taskFrameworkProperties;
        this.configProperties = configProperties;
    }

    @Override
    public List<String> hostUrl() {

        if (StringUtils.isNotBlank(taskFrameworkProperties.get().getOdcUrl())) {
            return Collections.singletonList(taskFrameworkProperties.get().getOdcUrl());
        }
        if (StringUtils.isNotBlank(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_SERVICE_HOST)) &&
                StringUtils.isNotBlank(SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_SERVER_PORT))) {
            return Collections
                    .singletonList("http://" + SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_SERVICE_HOST)
                            + ":" + SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_SERVICE_PORT));
        }

        String host =
                configProperties.getOdcHost() == null ? SystemUtils.getLocalIpAddress() : configProperties.getOdcHost();
        int port =
                configProperties.getPort() == null ? JobUtils.getPort() : Integer.parseInt(configProperties.getPort());
        return Collections.singletonList("http://" + host + ":" + port);

    }

}
