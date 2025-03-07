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

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceEndPoint;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
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
public class K8sPodResource implements Resource {
    /**
     * job region
     */
    private String region;

    /**
     * group of the job
     */
    private String group;

    /**
     * type of this k8sResource
     */
    private String type;

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

    /**
     * port of task supervisor listen for
     */
    private String servicePort;

    private Date createDate;

    public ResourceID resourceID() {
        return new ResourceID(new ResourceLocation(region, group), type, namespace, arn);
    }

    public String type() {
        return type;
    }

    public ResourceEndPoint endpoint() {
        StringBuilder sb = new StringBuilder();
        sb.append("k8s").append("::")
                .append(region).append("::")
                .append(namespace).append("::")
                .append(arn).append("::")
                .append(podIpAddress).append("::")
                .append(servicePort);
        return new ResourceEndPoint(sb.toString());
    }

    public static Pair<String, String> parseIPAndPort(String k8sEndPoint) {
        String[] infos = StringUtils.split(k8sEndPoint, "::");
        if (null == infos || infos.length != 6) {
            throw new IllegalStateException(
                    "expect k8s endpoint constructed by k8s::region::namespace::arn::ip::port, but current is "
                            + k8sEndPoint);
        }
        String host = "null".equalsIgnoreCase(infos[4]) ? null : infos[4];
        String port = "null".equalsIgnoreCase(infos[5]) ? null : infos[5];
        return Pair.of(host, port);
    }

    public ResourceState resourceState() {
        return resourceState;
    }

    public Date createDate() {
        return createDate;
    }
}
