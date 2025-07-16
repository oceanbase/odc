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

import com.oceanbase.odc.common.util.SystemUtils;
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

    public static Pair<String, String> parseIpv4IPAndPort(String k8sEndPoint) {
        String[] infos = StringUtils.splitByWholeSeparator(k8sEndPoint, "::");
        if (null == infos || infos.length != 6) {
            throw new IllegalStateException(
                    "expect k8s endpoint constructed by k8s::region::namespace::arn::ip::port, but current is "
                            + k8sEndPoint);
        }
        String host = "null".equalsIgnoreCase(infos[4]) ? null : infos[4];
        String port = "null".equalsIgnoreCase(infos[5]) ? null : infos[5];
        return Pair.of(host, port);
    }


    public static Pair<String, String> parseIPAndPort(String k8sEndPoint) {
        if (StringUtils.isEmpty(k8sEndPoint)) {
            throw new IllegalArgumentException("K8s endpoint cannot be null or empty");
        }

        if (!isIpv6Endpoint(k8sEndPoint)) {
            return parseIpv4IPAndPort(k8sEndPoint);
        }

        int ipv6StartIndex = k8sEndPoint.indexOf("::[");
        if (ipv6StartIndex == -1) {
            throw new IllegalArgumentException("Invalid IPv6 k8s endpoint format: " + k8sEndPoint);
        }

        int ipv6EndIndex = k8sEndPoint.indexOf("]::", ipv6StartIndex);
        if (ipv6EndIndex == -1) {
            throw new IllegalArgumentException("Invalid IPv6 k8s endpoint format, missing ']::': " + k8sEndPoint);
        }

        String ipPart = k8sEndPoint.substring(ipv6StartIndex + 3, ipv6EndIndex);

        String portPart = k8sEndPoint.substring(ipv6EndIndex + 3);

        String host = parseIPAddress(ipPart);

        String port = "null".equalsIgnoreCase(portPart) ? null : portPart;

        return Pair.of(host, port);
    }

    private static boolean isIpv6Endpoint(String endpoint) {
        return endpoint.contains("::[") && endpoint.contains("]::");
    }

    private static String parseIPAddress(String ipPart) {
        if (StringUtils.isEmpty(ipPart) || "null".equalsIgnoreCase(ipPart)) {
            return null;
        }

        // IPv6地址可能带有方括号，需要移除
        // 例如：[2001:db8::1] -> 2001:db8::1
        if (ipPart.startsWith("[") && ipPart.endsWith("]")) {
            return ipPart.substring(1, ipPart.length() - 1);
        }

        return ipPart;
    }

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
                .append(SystemUtils.addBracketsToIpv6AddressIfNeed(podIpAddress)).append("::")
                .append(servicePort);

        return new ResourceEndPoint(sb.toString());
    }

    public ResourceState resourceState() {
        return resourceState;
    }

    public Date createDate() {
        return createDate;
    }
}
