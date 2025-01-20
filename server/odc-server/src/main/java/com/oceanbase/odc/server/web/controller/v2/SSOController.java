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
package com.oceanbase.odc.server.web.controller.v2;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.SSOCredential;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.oauth2.SSOStateManager;
import com.oceanbase.odc.service.integration.oauth2.SSOTestInfo;
import com.oceanbase.odc.service.integration.oauth2.TestLoginManager;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

@RestController
@RequestMapping("/api/v2/sso")
public class SSOController {

    @Autowired
    private TestLoginManager testLoginManager;

    @Autowired
    private SSOStateManager SSOStateManager;

    @Autowired
    private IntegrationService integrationService;

    @PostMapping(value = "/test/start")
    public SuccessResponse<SSOTestInfo> addTestClientRegistration(@RequestBody IntegrationConfig config,
            @RequestParam(required = false) String type, @RequestParam(required = false) String odcBackUrl) {
        return Responses.ok(testLoginManager.getSSOTestInfo(config, type, odcBackUrl));
    }

    /**
     * 
     * @param testId {@link SSOTestInfo#getTestId()}
     * @return test login user profile
     */
    @GetMapping(value = "/test/info")
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#testId")
    public SuccessResponse<String> testUserInfo(String testId) {
        return Responses.ok(testLoginManager.getTestUserInfo(testId));
    }


    @GetMapping("/state")
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#state")
    public SuccessResponse<Map<String, String>> getTestClientInfo(@RequestParam String state) {
        return Responses.ok(SSOStateManager.getStateParameters(state));
    }

    @GetMapping("/credential")
    public SuccessResponse<SSOCredential> generateSSOCredential() {
        return Responses.ok(integrationService.generateSSOCredential());
    }

}
