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

import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2022/6/1 下午4:10
 * @Description: []
 */
@Service
@SkipAuthorize("odc internal usage")
public class CaptchaService {

    private final VerificationCodeGenerator verificationCodeGenerator = new ImageVerificationCodeGenerator();

    public VerificationCode createVerificationCode() {
        return verificationCodeGenerator.generateVerificationCode();
    }

    public boolean verify(@NonNull VerificationCode verificationCode, @NonNull String codeToVerify) {
        return StringUtils.equals(codeToVerify, verificationCode.getValue());
    }
}
