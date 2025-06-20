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
package com.oceanbase.odc.service.task.supervisor.runtime;

import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.SystemUtils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author longpeng.zlp
 * @date 2024/12/31 17:49
 */
@Data
@AllArgsConstructor
public class EndpointInfo {
    public static final EndpointInfo INVALID_END_POINT = new EndpointInfo("unknown", -1L, -1L);
    private String systemType;
    private Long memTotalMB;
    private Long memFreeMB;

    public EndpointInfo() {}

    public static EndpointInfo getEndpointInfo() {
        String osName = System.getProperty("os.name");
        Long memTotalMB = SystemUtils.getSystemTotalPhysicalMemory().convert(BinarySizeUnit.MB).getSizeDigit();
        Long memFreeMb = SystemUtils.getSystemFreePhysicalMemory().convert(BinarySizeUnit.MB).getSizeDigit();;
        return new EndpointInfo(osName, memTotalMB, memFreeMb);
    }


}
