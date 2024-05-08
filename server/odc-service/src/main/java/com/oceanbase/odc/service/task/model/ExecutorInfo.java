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
package com.oceanbase.odc.service.task.model;

import java.io.Serializable;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.model.HostProperties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(exclude = {"pid", "jvmStartTime"})
public class ExecutorInfo implements Serializable {

    private static final long serialVersionUID = -5583825823147254396L;
    private String host;
    private Integer port;
    private String hostName;
    private long pid;
    private long jvmStartTime;

    public ExecutorInfo() {}

    public ExecutorInfo(@NonNull HostProperties properties) {
        this.host = properties.getOdcHost() == null ? SystemUtils.getLocalIpAddress() : properties.getOdcHost();
        Verify.notNull(properties.getPort(), "Port");
        this.port = properties.getOdcMappingPort() == null ? Integer.parseInt(properties.getPort())
                : Integer.parseInt(properties.getOdcMappingPort());
        this.hostName = SystemUtils.getHostName();
        this.pid = SystemUtils.getPid();
        this.jvmStartTime = SystemUtils.getJVMStartTime();
    }
}
