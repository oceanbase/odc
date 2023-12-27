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
package com.oceanbase.odc.server.web.controller.v1;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.connection.model.MultiSessionsReq;
import com.oceanbase.odc.service.monitor.MemUnitType;
import com.oceanbase.odc.service.monitor.MetaInfo;
import com.oceanbase.odc.service.monitor.MonitorService;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v1/heartbeat")
public class HeartbeatController {
    /**
     * ODC监控相关的服务
     */
    @Autowired
    private MonitorService service;

    @ApiOperation(value = "heartbeat", notes = "前端心跳检测，sid示例：sid:1000-1:d:db1")
    @RequestMapping(method = RequestMethod.POST)
    public OdcResult<Set<String>> heartbeat(@RequestBody MultiSessionsReq req) {
        return OdcResult.ok(req.getSessionIds());
    }

    @ApiOperation(value = "isHealthy", notes = "探测服务是否存活")
    @RequestMapping(value = "/isHealthy", method = RequestMethod.GET)
    public OdcResult<Boolean> isHealthy() {
        OdcResult<Boolean> result = new OdcResult<>();
        result.setData(true);
        AlarmUtils.info(AlarmEventNames.IS_HEALTHY, "OK");
        return result;
    }

    /**
     * 获取ODC当亲的内存使用状态接口
     *
     * @return 返回当前的系统状态
     */
    @ApiOperation(value = "resource use", notes = "查看目前ODC的资源使用情况")
    @RequestMapping(value = "/getMetaStatus", method = RequestMethod.GET)
    public OdcResult<MetaInfo> getStatus(
            @RequestParam(name = "unitOfMem", defaultValue = "BYTE", required = false) String unitOfMem) {
        MemUnitType type = MemUnitType.valueOf(unitOfMem);
        return service.getOdcMetaInfo(type);
    }

}
