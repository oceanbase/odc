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

package com.oceanbase.odc.service.task.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JvmJobCaller;
import com.oceanbase.odc.service.task.caller.K8sJobCaller;
import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.caller.NativeK8sJobClient;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.enums.DeployModelEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
@Slf4j
@Configuration
public class TaskFrameworkConfiguration {

    @Bean
    @ConditionalOnMissingBean(K8sJobClient.class)
    public K8sJobClient k8sJobClient(@Autowired TaskFrameworkProperties taskFrameworkProperties) {
        if (taskFrameworkProperties.getDeployModel() == DeployModelEnum.K8S) {
            try {
                log.info("k8s url is {}", taskFrameworkProperties.getK8s().getUrl());
                return new NativeK8sJobClient(taskFrameworkProperties.getK8s().getUrl());
            } catch (IOException e) {
                log.warn("Create NativeK8sJobClient occur error", e);
            }
        }
        return null;
    }

    @Bean
    public JobCaller jobCaller(@Autowired(required = false) K8sJobClient k8sJobClient,
            @Autowired TaskFrameworkProperties taskFrameworkProperties) {
        switch (taskFrameworkProperties.getDeployModel()) {
            case STANDALONE:
                return new JvmJobCaller();
            case K8S:
            default:
                return getK8sJobCaller(k8sJobClient);
        }
    }

    @Bean
    public JobConfigurationHolder jobConfigurationHolder() {
        return new JobConfigurationHolder();
    }

    private K8sJobCaller getK8sJobCaller(K8sJobClient k8sJobClient) {
        if (k8sJobClient == null) {
            throw new BeanCreationException("Current deploy model is k8s, but k8sJobClient is missing");
        }
        PodConfig podConfig = new PodConfig();
        // todo replaced by odc-executor
        String imageName = "perl:5.34.0";
        List<String> cmd = Arrays.asList("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)");
        podConfig.setImage(imageName);
        podConfig.setCommand(cmd);
        podConfig.setNamespace("default");
        return new K8sJobCaller(k8sJobClient, podConfig);
    }
}
