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


/**
 * @author longpeng.zlp
 * @date 2024/9/2 17:15
 */
public interface K8sResourceOperatorBuilder {
    /**
     * context to build operator
     * 
     * @param region region of the k8s cluster
     * @param group group of the k8s cluster
     * @return k8s operator to manipulate k8s cluster
     */
    K8SResourceOperator build(String region, String group);
}
