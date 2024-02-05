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
package com.oceanbase.odc.service.dispatch;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JacksonFactory;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.response.ErrorResponse;
import com.oceanbase.odc.service.common.response.OdcResult;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Response for {@code RPC}
 *
 * @author yh263208
 * @date 2022-03-28 19:12
 * @since ODC_release_3.3.0
 */
@Slf4j
public class DispatchResponse {
    @Getter
    private final byte[] content;
    private ErrorResponseException thrown;
    @Getter
    private final HttpHeaders responseHeaders;
    @Getter
    private final HttpStatus httpStatus;

    public static DispatchResponse of(@NonNull byte[] content, @NonNull HttpHeaders headers,
            @NonNull HttpStatus httpStatus) {
        DispatchResponse response = new DispatchResponse(content, headers, httpStatus);
        if (response.thrown != null) {
            throw response.thrown;
        }
        return response;
    }

    private DispatchResponse(@NonNull byte[] content, @NonNull HttpHeaders responseHeaders,
            @NonNull HttpStatus httpStatus) {
        this.content = content;
        this.responseHeaders = responseHeaders;
        this.httpStatus = httpStatus;
        MediaType contentType = responseHeaders.getContentType();
        Verify.notNull(contentType, "ContentType can not be null");
        this.thrown = null;
        if (!MediaType.APPLICATION_JSON.equalsTypeAndSubtype(contentType)) {
            return;
        }
        String contentStr;
        Charset charset = contentType.getCharset();
        if (charset != null) {
            contentStr = new String(content, charset);
        } else {
            contentStr = new String(content);
        }
        HttpStatus jsonHttpStatus;
        String errorCode;
        String localizedMessage;
        OdcResult<?> v1Result = v1(contentStr);
        if (v1Result.getErrCode() != null && v1Result.getErrMsg() != null) {
            jsonHttpStatus = v1Result.getHttpStatus();
            errorCode = v1Result.getErrCode();
            localizedMessage = v1Result.getErrMsg();
        } else {
            ErrorResponse v2Result = v2(contentStr);
            if (v2Result.getError() == null) {
                return;
            }
            jsonHttpStatus = v2Result.getHttpStatus();
            errorCode = v2Result.getError().getCode();
            localizedMessage = v2Result.getError().getMessage();
        }
        thrown = new ErrorResponseException(jsonHttpStatus, errorCode, localizedMessage);
    }

    public <T> T getContentByType(@NonNull TypeReference<T> reference) {
        String contentJson = encodeContentToString();
        try {
            return JacksonFactory.jsonMapper().readValue(contentJson, reference);
        } catch (JsonProcessingException e) {
            log.warn("Deserialization error, contentJson={}", contentJson, e);
            throw new IllegalStateException(e);
        }
    }

    private OdcResult<?> v1(String responseBody) {
        return JsonUtils.fromJson(responseBody, OdcResult.class);
    }

    private ErrorResponse v2(String responseBody) {
        return JsonUtils.fromJson(responseBody, ErrorResponse.class);
    }

    private String encodeContentToString() {
        MediaType contentType = responseHeaders.getContentType();
        Verify.notNull(contentType, "ContentType can not be null");
        if (!MediaType.APPLICATION_JSON.equalsTypeAndSubtype(contentType)) {
            throw new IllegalStateException("Wrong media type " + contentType);
        }
        Charset charset = contentType.getCharset();
        if (charset == null) {
            return new String(getContent());
        }
        return new String(getContent(), charset);
    }

}
