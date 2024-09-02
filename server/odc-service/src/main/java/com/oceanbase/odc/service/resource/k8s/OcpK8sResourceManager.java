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
package com.oceanbase.odc.service.resource.k8s;

import com.oceanbase.odc.metadb.resource.ResourceRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * resource manager to holds resource allocate and free
 * 
 * @author longpeng.zlp
 * @date 2024/8/26 20:17
 */
@Slf4j
public class OcpK8sResourceManager extends K8sResourceManager {
    /**
     * map to hold remote k8s resource operator key is region value is k8s cluster
     */
    private K8sResourceOperatorBuilder ocpK8sResourceOperatorBuilder;

    public OcpK8sResourceManager(ResourceRepository resourceMetaStore) {
        super(resourceMetaStore);
    }

    /**
     * register or update k8s operator
     */
    public void registerK8sOperator(String type, K8sResourceOperatorBuilder operatorBuilder) {
        K8sResourceOperatorBuilder prev = ocpK8sResourceOperatorBuilder;
        this.ocpK8sResourceOperatorBuilder = operatorBuilder;
        if (null == prev) {
            log.info("k8s operator registered for type={}, operator={}", type, operatorBuilder);
        } else {
            log.info("k8s operator updated for type={}, current={}, prev={}", type, operatorBuilder, prev);
        }
    }

    /**
     * find resource operator by region
     * 
     * @param region
     * @return
     */
    protected K8SResourceOperator getK8sOperator(String region, String group) {
        K8SResourceOperator operator = ocpK8sResourceOperatorBuilder.build(region, group);
        if (null == operator) {
            throw new IllegalStateException("k8s operator for region= " + region + " not found");
        }
        return operator;
    }
}
