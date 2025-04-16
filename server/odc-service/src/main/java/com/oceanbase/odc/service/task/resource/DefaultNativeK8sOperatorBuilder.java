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
package com.oceanbase.odc.service.task.resource;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.dummy.LocalMockK8sJobClient;
import com.oceanbase.odc.service.task.resource.client.DefaultK8sJobClientSelector;
import com.oceanbase.odc.service.task.resource.client.K8sJobClientSelector;
import com.oceanbase.odc.service.task.resource.client.NativeK8sJobClient;

import lombok.extern.slf4j.Slf4j;

/**
 * native k8s operator
 * 
 * @author longpeng.zlp
 * @date 2025/1/22 13:57
 */
@Slf4j
public class DefaultNativeK8sOperatorBuilder extends AbstractK8sResourceOperatorBuilder {
    public static final String NATIVE_K8S_POD_TYPE = "nativeK8sPod";

    public DefaultNativeK8sOperatorBuilder(@Autowired TaskFrameworkProperties taskFrameworkProperties,
            @Autowired ResourceRepository resourceRepository) throws IOException {
        super(taskFrameworkProperties, resourceRepository, NATIVE_K8S_POD_TYPE);
    }

    /**
     * build k8s job selector
     */
    protected K8sJobClientSelector buildK8sJobSelector(
            TaskFrameworkProperties taskFrameworkProperties) throws IOException {
        K8sProperties k8sProperties = taskFrameworkProperties.getK8sProperties();
        K8sJobClientSelector k8sJobClientSelector;
        if (taskFrameworkProperties.isEnableK8sLocalDebugMode()) {
            // k8s use in local debug mode
            log.info("local debug k8s cluster enabled.");
            k8sJobClientSelector = new LocalMockK8sJobClient();
        } else {
            // normal mode
            log.info("build k8sJobClientSelector, kubeUrl={}, namespace={}",
                    k8sProperties.getKubeUrl(), k8sProperties.getNamespace());
            NativeK8sJobClient nativeK8sJobClient = new NativeK8sJobClient(k8sProperties);
            k8sJobClientSelector = new DefaultK8sJobClientSelector(nativeK8sJobClient);
        }
        return k8sJobClientSelector;
    }
}
