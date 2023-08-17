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
package com.oceanbase.odc.service.onlineschemachange.oms.client;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.validate.ValidatorUtils;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.oms.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.oms.request.BaseOmsRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Component
@Slf4j
public class DefaultPrivateCloudOmsClient implements OmsClient {

    private final RestTemplate omsRestTemplate;
    private final OnlineSchemaChangeProperties onlineSchemaChangeProperties;

    public DefaultPrivateCloudOmsClient(
            @Autowired OnlineSchemaChangeProperties onlineSchemaChangeProperties,
            @Autowired RestTemplate omsRestTemplate) {
        this.onlineSchemaChangeProperties = onlineSchemaChangeProperties;
        this.omsRestTemplate = omsRestTemplate;
    }

    public <T> T postOmsInterface(ClientRequestParams params) {
        ValidatorUtils.verifyField(params.getRequest());
        OmsApiReturnResult<T> result = resolveResponseEntity(params,
                doPostOmsInterface(params.getRequest(), params.getAction()), params.getTypeReference());
        return result.getData();
    }

    private ResponseEntity<String> doPostOmsInterface(BaseOmsRequest request, String action) {
        String realUrl = onlineSchemaChangeProperties.getOms().getUrl() + "/api/v2?Action=" + action;
        ResponseEntity<String> responseEntity;
        if (log.isDebugEnabled()) {
            log.debug("Prepare request to the oms interface, url={}, request body={}", realUrl,
                    StringUtils.singleLine(JsonUtils.toJson(request)));
        }
        responseEntity = exchange(request, realUrl);
        if (log.isDebugEnabled()) {
            log.debug("Successfully response from the oms interface, url={}, response body ={}", realUrl,
                    StringUtils.singleLine(JsonUtils.toJson(responseEntity)));
        }
        return responseEntity;
    }

    private ResponseEntity<String> exchange(BaseOmsRequest request, String realUrl) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (onlineSchemaChangeProperties.getOms().getAuthorization() != null) {
            httpHeaders.setBasicAuth(onlineSchemaChangeProperties.getOms().getAuthorization());
        }
        HttpEntity<BaseOmsRequest> httpRequest = new HttpEntity<>(request, httpHeaders);
        return omsRestTemplate.exchange(realUrl, HttpMethod.POST, httpRequest, String.class);
    }

    private <T> OmsApiReturnResult<T> resolveResponseEntity(ClientRequestParams requestParams,
            ResponseEntity<String> responseEntity, TypeReference<OmsApiReturnResult<T>> typeReference) {
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new OmsException(responseEntity.toString());
        }
        OmsApiReturnResult<T> result = JsonUtils.fromJson(responseEntity.getBody(), typeReference);
        if (result == null) {
            throw new OmsException("Parse oms result occur error, result=" + responseEntity.getBody());
        }
        if (!result.isSuccess()) {
            String msg = MessageFormat.format(
                    "Failed response oms result,Action={0}, Request Params={1}, Response={2}",
                    requestParams.getAction(), JsonUtils.toJson(requestParams.getRequest()), result.getMessage());
            throw new OmsException(result.getCode(), msg, result.getRequestId());
        }
        if (log.isDebugEnabled()) {
            log.debug("Successfully response oms result, oms internal cost={}", result.getCost());
        }
        return result;
    }
}
