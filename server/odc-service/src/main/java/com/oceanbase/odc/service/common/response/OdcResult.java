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
package com.oceanbase.odc.service.common.response;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = {"data", "code", "message"})
public class OdcResult<T> {
    private String errCode;
    private String errMsg;
    /**
     * same as errCode, for match OBCLoud criteria
     */
    private String code;
    /**
     * same as errMsg, for match OBCLoud criteria
     */
    private String message;

    private T data;
    private boolean isImportantMsg = false;

    private OffsetDateTime timestamp;
    private Long durationMillis;
    private String traceId;
    private String server;
    private String requestId;
    private HttpStatus httpStatus;

    public OdcResult() {}

    public OdcResult(T data) {
        this.data = data;
    }

    public static <T> OdcResult<T> ok(T t) {
        OdcResult<T> result = new OdcResult<>(t);
        result.setHttpStatus(HttpStatus.OK);
        return result;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
        this.code = errCode;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        this.message = errMsg;
    }
}
