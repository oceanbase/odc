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
package com.oceanbase.odc.service.state;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.model.HostProperties;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class RouteInfo {

    private static final RestTemplate restTemplate = new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(5)).build();

    private String hostName;
    private Integer port;


    public RouteInfo(String hostName, Integer port) {
        this.hostName = hostName;
        this.port = port;
    }

    public boolean isCurrentNode(@NonNull HostProperties properties) {
        Integer currentNodePort = properties.getRequestPort();
        Verify.notNull(properties.getPort(), "Port");
        if (!Objects.equals(port, currentNodePort)) {
            return false;
        }
        return hostName.equalsIgnoreCase(currentNodeHostName())
                || hostName.equalsIgnoreCase(currentNodeIpAddress(properties));
    }

    public static String currentNodeHostName() {
        return SystemUtils.getHostName();
    }

    public static String currentNodeIpAddress(HostProperties properties) {
        return properties.getOdcHost() == null ? SystemUtils.getLocalIpAddress() : properties.getOdcHost();
    }

    public boolean isHealthyHost() {
        try {
            String url = "http://" + hostName + ":" + port + "/api/v1/heartbeat/isHealthy";
            String response = restTemplate.getForObject(url, String.class);
            return response.contains("true");
        } catch (Exception e) {
            return false;
        }
    }

    public String toString() {
        return hostName + ":" + port;
    }
}
