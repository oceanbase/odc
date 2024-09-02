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

import com.oceanbase.odc.metadb.resource.GlobalUniqueResourceID;
import com.oceanbase.odc.service.resource.ResourceID;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * global unique resource ID
 * 
 * @author longpeng.zlp
 * @date 2024/8/12 11:30
 */
@EqualsAndHashCode
@ToString
public class K8sResourceID extends GlobalUniqueResourceID implements ResourceID {
    public K8sResourceID(String region, String group, String namespace, String name) {
        super(region, group, namespace, name);
    }
}
