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
package com.oceanbase.odc.service.dispatch;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2024-01-23
 * @since 4.2.4
 */
@Component
@Profile("alipay")
public class WebJobDispatcherChecker implements JobDispatchChecker {

    @Autowired
    private HostProperties hostProperties;

    @Override
    public boolean isExecutorOnThisMachine(@NonNull JobEntity je) {
        if (JobUtils.isK8sRunMode(je.getRunMode())) {
            return true;
        }
        String identifier = je.getExecutorIdentifier();
        if (identifier == null) {
            return true;
        }

        ExecutorIdentifier ei = ExecutorIdentifierParser.parser(identifier);
        String host =
                hostProperties.getOdcHost() == null ? SystemUtils.getLocalIpAddress() : hostProperties.getOdcHost();
        return Objects.equals(host, ei.getHost());
    }
}
