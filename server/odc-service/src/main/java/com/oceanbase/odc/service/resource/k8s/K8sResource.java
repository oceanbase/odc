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

import java.util.Date;

import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceEndPoint;
import com.oceanbase.odc.service.resource.ResourceMode;
import com.oceanbase.odc.service.resource.ResourceState;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * resource allocate from k8s
 * 
 * @author longpeng.zlp
 * @date 2024/8/12 14:27
 */
@Data
@AllArgsConstructor
public class K8sResource implements Resource<K8sResourceID> {
    /**
     * job region
     */
    private String region;

    /**
     * group of the job
     */
    private String group;

    /**
     * namespace of k8s
     */
    private String namespace;

    /**
     * job identity string
     */
    private String arn;

    /**
     * resource state
     */
    private ResourceState resourceState;

    /**
     * pod ip address
     */
    private String podIpAddress;

    private Date createDate;

    public K8sResourceID id() {
        return new K8sResourceID(region, group, namespace, arn);
    }

    public ResourceMode type() {
        return ResourceMode.LOCAL_PROCESS;
    }

    public ResourceEndPoint endpoint() {
        StringBuilder sb = new StringBuilder();
        sb.append("k8s").append("::")
                .append(region).append("::")
                .append(namespace).append("::")
                .append(arn).append("::")
                .append(podIpAddress);
        return new ResourceEndPoint(sb.toString());
    }

    @Override
    public ResourceState resourceState() {
        return resourceState;
    }

    @Override
    public Date createDate() {
        return createDate;
    }
}
