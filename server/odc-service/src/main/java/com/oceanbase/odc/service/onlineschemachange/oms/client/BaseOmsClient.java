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
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.validate.ValidatorUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.onlineschemachange.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.oms.request.BaseOmsRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-16
 * @since 4.2.0
 */
@Slf4j
public abstract class BaseOmsClient implements OmsClient {
    private final RestTemplate omsRestTemplate;
    private final String url;

    protected BaseOmsClient(String url, RestTemplate omsRestTemplate) {
        this.omsRestTemplate = omsRestTemplate;
        this.url = url;
    }

    @Override
    public <T> T postOmsInterface(ClientRequestParams params) {
        ValidatorUtils.verifyField(params.getRequest());
        OmsApiReturnResult<T> result = resolveResponseEntity(params,
                doPostOmsInterface(params.getRequest(), params.getAction()), params.getTypeReference());
        return result.getData();
    }

    private ResponseEntity<String> doPostOmsInterface(BaseOmsRequest request, String action) {
        String realUrl = url + "?Action=" + action;
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
        setHttpHeaders(httpHeaders);
        HttpEntity<BaseOmsRequest> httpRequest = new HttpEntity<>(request, httpHeaders);
        return omsRestTemplate.exchange(realUrl, HttpMethod.POST, httpRequest, String.class);
    }

    @VisibleForTesting
    protected <T> OmsApiReturnResult<T> resolveResponseEntity(ClientRequestParams requestParams,
            ResponseEntity<String> responseEntity, TypeReference<OmsApiReturnResult<T>> typeReference) {
        log.info("process oms request [{}] with response [{}]", url + "/" + requestParams, responseEntity.getBody());
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            if (responseEntity.getStatusCode() == HttpStatus.REQUEST_TIMEOUT) {
                throw new OmsException(ErrorCodes.Timeout, responseEntity.toString(), null,
                        responseEntity.getStatusCode());
            }

            ErrorCode errorCode = responseEntity.getStatusCode().is5xxServerError() ? ErrorCodes.ExternalServiceError
                    : ErrorCodes.BadRequest;
            throw new OmsException(errorCode, responseEntity.toString(), null, responseEntity.getStatusCode());
        }
        OmsApiReturnResult<T> result = JsonUtils.fromJsonIgnoreMissingProperty(responseEntity.getBody(), typeReference);
        if (result == null) {
            throw new UnexpectedException("Parse oms result occur error, result=" + responseEntity.getBody());
        }
        resolveFailedResult(requestParams, responseEntity, result);

        if (log.isDebugEnabled()) {
            log.debug("Successfully response oms result, oms internal cost={}", result.getCost());
        }
        return result;
    }

    private <T> void resolveFailedResult(ClientRequestParams requestParams, ResponseEntity<String> responseEntity,
            OmsApiReturnResult<T> result) {
        if (result.isSuccess()) {
            return;
        }
        cleanUpSensitiveMsgInRequest(requestParams);
        String msg = MessageFormat.format(
                "Failed response oms result,Action={0}, Request Params={1}, Response={2}",
                requestParams.getAction(), JsonUtils.toJson(requestParams.getRequest()), result);
        if (result.getCode() != null) {
            if (Objects.equals("PARAM_ERROR", result.getCode()) &&
                    result.getMessage().startsWith("Connectivity test failed")) {
                throw new OmsException(ErrorCodes.OmsConnectivityTestFailed, msg, result.getRequestId(),
                        responseEntity.getStatusCode());
            } else if (Objects.equals("PARAM_ERROR", result.getCode())) {
                throw new OmsException(ErrorCodes.OmsParamError, msg, result.getRequestId(),
                        responseEntity.getStatusCode());
            } else if (result.getCode().startsWith("GHANA-OPERAT")) {
                throw new OmsException(ErrorCodes.OmsGhanaOperateFailed, msg, result.getRequestId(),
                        responseEntity.getStatusCode());
            }
        }
        throw new OmsException(ErrorCodes.BadArgument, msg, result.getRequestId(),
                responseEntity.getStatusCode());
    }

    private void cleanUpSensitiveMsgInRequest(ClientRequestParams requestParams) {
        if ("CreateOceanBaseDataSource".equals(requestParams.getAction())
                && requestParams.getRequest() instanceof CreateOceanBaseDataSourceRequest) {
            CreateOceanBaseDataSourceRequest request = (CreateOceanBaseDataSourceRequest) requestParams.getRequest();
            CreateOceanBaseDataSourceRequest copiedRequest = new CreateOceanBaseDataSourceRequest();
            BeanUtils.copyProperties(request, copiedRequest);
            // Clean password in log
            copiedRequest.setPassword(null);
            copiedRequest.setDrcPassword(null);
            requestParams.setRequest(copiedRequest);
        }
    }

    protected abstract void setHttpHeaders(HttpHeaders httpHeaders);

}
