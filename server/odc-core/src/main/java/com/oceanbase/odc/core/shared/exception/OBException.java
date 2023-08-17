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
package com.oceanbase.odc.core.shared.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;

/**
 * @author yizhou.xw
 * @version : ObException.java, v 0.1 2021-03-20 19:26
 */
public class OBException extends HttpException {
    private final HttpStatus httpStatus;

    public OBException(ErrorCode errorCode, Object[] args, String message, HttpStatus httpStatus) {
        super(errorCode, args, message);
        this.httpStatus = httpStatus;
    }

    public OBException(ErrorCode errorCode, Object[] args, String message, HttpStatus httpStatus, Throwable throwable) {
        super(errorCode, args, message, throwable);
        this.httpStatus = httpStatus;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    /**
     * 对于ODC而言，数据库服务端无论返回什么信息，HTTP层面来说请求都是处理成功的
     */
    public static OBException accessDenied(DialectType dialectType, String connectSchema, String message) {
        if (Objects.nonNull(dialectType) && dialectType.isOBMysql()) {
            return new OBException(ErrorCodes.ObMysqlAccessDenied,
                    new Object[] {connectSchema, message}, message, HttpStatus.OK);
        }
        return new OBException(ErrorCodes.ObAccessDenied,
                new Object[] {message}, message, HttpStatus.OK);
    }

    public static OBException commandDenied(String message) {
        return new OBException(ErrorCodes.ObCommandDenied, new Object[] {message}, message, HttpStatus.OK);
    }

    public static OBException connectFailed(String message) {
        return new OBException(ErrorCodes.ObConnectFailed, new Object[] {message}, message, HttpStatus.OK);
    }

    public static OBException executeFailed(String message) {
        return new OBException(ErrorCodes.ObExecuteSqlFailed, new Object[] {message}, message, HttpStatus.OK);
    }

    public static OBException executeFailed(ErrorCode errorCode, String message) {
        return new OBException(errorCode, new Object[] {message}, message, HttpStatus.OK);
    }

    public static OBException executeFailed(ErrorCode errorCode, Object[] args, String message) {
        return new OBException(errorCode, args, message, HttpStatus.OK);
    }

    public static OBException executePlFailed(String message) {
        return new OBException(ErrorCodes.ObExecutePlFailed, new Object[] {message}, message, HttpStatus.OK);
    }

    public static OBException featureNotSupported(String message) {
        return new OBException(ErrorCodes.ObFeatureNotSupported, new Object[] {message}, message,
                HttpStatus.BAD_REQUEST);
    }

    public static OBException invalidCreateSqlCondition(String message) {
        return new OBException(ErrorCodes.ObInvalidCreateSqlCondition, new Object[] {message}, message,
                HttpStatus.BAD_REQUEST);
    }

    public static OBException globalVariableSetSessionScopeNotSupported(String message) {
        return new OBException(ErrorCodes.ObGlobalVariableSetSessionScopeNotSupported, new Object[] {message}, message,
                HttpStatus.OK);
    }
}
