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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.encryption.EncryptionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author gaoda.xy
 * @date 2023/8/25 10:01
 */
@RestController
@RequestMapping("/api/v2/encryption")
public class EncryptionController {

    @Autowired
    private EncryptionService encryptionService;

    @ApiOperation(value = "getPublicKey", notes = "Get public key for encryption")
    @RequestMapping(value = "/publicKey", method = RequestMethod.GET)
    public SuccessResponse<String> getPublicKey() {
        return Responses.success(encryptionService.getPublicKey());
    }

}
