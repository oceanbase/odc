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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.llm.LlmService;
import com.oceanbase.odc.service.llm.model.LlmModel;
import com.oceanbase.odc.service.llm.model.ModelCredentialDto;
import com.oceanbase.odc.service.llm.model.ProviderCredentialDto;
import com.oceanbase.odc.service.llm.model.ProviderDescription;
import com.oceanbase.odc.service.llm.model.ProviderType;
import com.oceanbase.odc.service.llm.model.SetDescriptionRequest;
import com.oceanbase.odc.service.llm.model.SetLlmModelEnabledRequest;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/v2/integration/llm")
public class LlmController {

    @Autowired
    private LlmService llmService;

    @ApiOperation(value = "getProviders", notes = "Get all providers")
    @GetMapping("/providers")
    public ListResponse<ProviderDescription> getProviders() {
        return Responses.list(llmService.getProviders());
    }

    @ApiOperation(value = "getModels", notes = "Get all models")
    @GetMapping("/providers/{provider}/models")
    public ListResponse<LlmModel> getModels(@PathVariable String provider) {
        return Responses.list(llmService.getModels(provider));
    }

    @ApiOperation(value = "getProviderCredential", notes = "Get provider credential")
    @GetMapping("/providers/{provider}")
    public SuccessResponse<ProviderCredentialDto> getProviderCredential(@PathVariable String provider) {
        return Responses.success(llmService.getProviderCredential(provider));
    }

    @ApiOperation(value = "setProviderCredential", notes = "Set provider credential")
    @PostMapping("/providers")
    public SuccessResponse<String> setProviderCredential(@RequestBody ProviderCredentialDto providerCredential) {
        return Responses.success(llmService.setProviderCredential(providerCredential));
    }

    @ApiOperation(value = "deleteProviderCredential", notes = "Delete provider credential")
    @DeleteMapping("/providers")
    public SuccessResponse<String> deleteProviderCredential(@RequestBody ProviderCredentialDto providerCredential) {
        return Responses.success(llmService.deleteProviderCredential(providerCredential));
    }

    @ApiOperation(value = "getModelCredential", notes = "Get model credential")
    @GetMapping("/providers/{provider}/models/{model}")
    public SuccessResponse<ModelCredentialDto> getModelCredential(@PathVariable String provider,
            @PathVariable String model) {
        return Responses.success(llmService.getModelCredential(provider, model));
    }

    @ApiOperation(value = "setModelCredential", notes = "Set model credential")
    @PostMapping("/providers/{provider}/models")
    public SuccessResponse<String> setModelCredential(@PathVariable String provider,
            @RequestBody ModelCredentialDto modelCredential) {
        modelCredential.setProvider(ProviderType.valueOf(provider));
        return Responses.success(llmService.setModelCredential(modelCredential));
    }

    @ApiOperation(value = "deleteModelCredential", notes = "Delete model credential")
    @DeleteMapping("/providers/{provider}/models")
    public SuccessResponse<String> deleteModelCredential(@PathVariable String provider,
            @RequestBody ModelCredentialDto modelCredential) {
        modelCredential.setProvider(ProviderType.valueOf(provider));
        return Responses.success(llmService.deleteModelCredential(modelCredential));
    }

    @ApiOperation(value = "setModelEnabled", notes = "Set model enabled")
    @PostMapping("/providers/{provider}/models/{modelName}/setEnabled")
    public SuccessResponse<String> setModelEnabled(@PathVariable String provider,
            @PathVariable String modelName,
            @RequestBody SetLlmModelEnabledRequest request) {
        return Responses.success(llmService.setModelEnabled(provider, modelName, request.getEnabled()));
    }

    @ApiOperation(value = "setProviderDescription", notes = "Set provider description")
    @PostMapping("/providers/{provider}/setDescription")
    public SuccessResponse<String> setProviderDescription(@PathVariable String provider,
            @RequestBody SetDescriptionRequest request) {
        return Responses.success(llmService.setProviderDescription(provider, request.getDescription()));
    }

}
