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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.info.OdcInfo;
import com.oceanbase.odc.service.info.OdcInfoService;

/**
 * @author yizhou.xw
 * @version : OdcInfoController.java, v 0.1 2021-02-05 16:35
 */
@RestController
@RequestMapping("/api/v1")
public class OdcInfoController {

    @Autowired
    private OdcInfoService infoService;

    @GetMapping(value = "/time")
    public OdcResult<String> time() {
        return OdcResult.ok(infoService.time().toString());
    }

    @GetMapping("/info")
    public OdcResult<OdcInfo> info() {
        return OdcResult.ok(infoService.info());
    }

    @GetMapping("/build-info")
    public BuildProperties buildInfo() {
        return infoService.buildInfo();
    }

    @GetMapping("/git-info")
    public GitProperties gitInfo() {
        return infoService.gitInfo();
    }

    @GetMapping(value = "/status", produces = "text/plain")
    public String status() {
        return infoService.status();
    }
}
