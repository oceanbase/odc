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

import org.apache.commons.lang.Validate;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.exception.HttpException;

/**
 * @author yizhou.xw
 * @version : OdcErrorResult.java, v 0.1 2021-03-08 16:05
 */
public class OdcErrorResult extends OdcResult<Boolean> {

    private OdcErrorResult() {
        super(false);
    }

    public static OdcErrorResult empty() {
        OdcErrorResult result = new OdcErrorResult();
        result.setErrCode("N/A");
        return result;
    }

    public static OdcErrorResult error(HttpException e) {
        Validate.notNull(e, "parameter e may not be null");
        OdcErrorResult result = new OdcErrorResult();
        result.setErrCode(e.getErrorCode().code());
        result.setErrMsg(e.getLocalizedMessage());
        result.setHttpStatus(e.httpStatus());
        return result;
    }

    public static OdcErrorResult error(ErrorCode errorCode) {
        return error(errorCode, null);
    }

    public static OdcErrorResult error(ErrorCode errorCode, Object[] args) {
        Validate.notNull(errorCode, "parameter errorCode may not be null");
        OdcErrorResult result = new OdcErrorResult();
        result.setErrCode(errorCode.code());
        result.setErrMsg(errorCode.getLocalizedMessage(args));
        result.setServer(SystemUtils.getHostName());
        return result;
    }
}
