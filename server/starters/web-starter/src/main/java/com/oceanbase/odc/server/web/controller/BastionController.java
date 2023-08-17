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
package com.oceanbase.odc.server.web.controller;

import javax.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.bastion.BastionEncryptionService;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;

import lombok.Data;

@Validated
@RestController
@RequestMapping("/api/v2/bastion")
public class BastionController {
    @Autowired
    private BastionEncryptionService bastionEncryptionService;

    @RequestMapping(value = "/encryption/encrypt", method = RequestMethod.POST)
    public SuccessResponse<String> encrypt(@RequestBody EncryptReq req) {
        return Responses.success(bastionEncryptionService.encrypt(req.getData()));
    }

    @RequestMapping(value = "/encryption/decrypt", method = RequestMethod.POST)
    public SuccessResponse<String> decrypt(@RequestBody DecryptReq req) {
        return Responses.success(bastionEncryptionService.decrypt(req.getData()));
    }

    @Data
    public static class EncryptReq {
        @NotBlank
        private String data;
    }
    @Data
    public static class DecryptReq {
        @NotBlank
        private String data;
    }
}
