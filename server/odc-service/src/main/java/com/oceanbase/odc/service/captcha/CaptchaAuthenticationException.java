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
package com.oceanbase.odc.service.captcha;

import org.springframework.security.core.AuthenticationException;

import com.oceanbase.odc.core.shared.constant.ErrorCode;

/**
 * @Author: Lebie
 * @Date: 2022/5/31 下午5:42
 * @Description: []
 */
public class CaptchaAuthenticationException extends AuthenticationException {
    private ErrorCode errorCode;

    public CaptchaAuthenticationException(ErrorCode errorCode, Object[] args) {
        super(errorCode.getLocalizedMessage(args));
        this.errorCode = errorCode;
    }

    public ErrorCode getCode() {
        return this.errorCode;
    }
}
