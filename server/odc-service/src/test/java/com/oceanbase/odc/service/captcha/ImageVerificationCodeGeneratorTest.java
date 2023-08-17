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

import static com.oceanbase.odc.service.captcha.CaptchaConstants.IMAGE_VERIFICATION_CODE_DIGITS;
import static com.oceanbase.odc.service.captcha.CaptchaConstants.IMAGE_VERIFICATION_CODE_EXPIRED_TIME_SECONDS;
import static org.junit.Assert.*;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

public class ImageVerificationCodeGeneratorTest {
    private static final ImageVerificationCodeGenerator generator = new ImageVerificationCodeGenerator();

    @Test
    public void testGenerateVerifyCode_DigitsEquals_ReturnTrue() {
        VerificationCode verificationCode = generator.generateVerificationCode();
        assertTrue(IMAGE_VERIFICATION_CODE_DIGITS == verificationCode.getValue().length());
    }

    @Test
    public void testGenerateVerifyCode_VerifyCodeIsExpired_ReturnFalse() {
        VerificationCode verificationCode = generator.generateVerificationCode();
        assertFalse(verificationCode.isExpired());
    }

    @Test
    public void testGenerateVerifyCode_VerifyCodeIsExpired_ReturnTrue() {
        Date now = new Date(1);
        VerificationCode verificationCode = generator.generateVerificationCode(now);
        Date dateAlreadyExpired = DateUtils.addSeconds(now, IMAGE_VERIFICATION_CODE_EXPIRED_TIME_SECONDS + 1);
        assertTrue(verificationCode.getExpiredTime().before(dateAlreadyExpired));
    }
}
