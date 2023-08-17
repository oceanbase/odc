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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/6/1 下午4:27
 * @Description: [图片验证码实现，使用 hotool-captcha 生成验证码]
 */
@Slf4j
public class ImageVerificationCodeGenerator implements VerificationCodeGenerator {
    @Override
    public VerificationCode generateVerificationCode() {
        // 定义图形验证码的长、宽、验证码字符数、干扰元素个数
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(CaptchaConstants.IMAGE_VERIFICATION_CODE_WIDTH,
                CaptchaConstants.IMAGE_VERIFICATION_CODE_HEIGHT,
                CaptchaConstants.IMAGE_VERIFICATION_CODE_DIGITS, CaptchaConstants.IMAGE_VERIFICATION_CODE_CIRCLE_COUNT);
        InputStream image = new ByteArrayInputStream(captcha.getImageBytes());
        return VerificationCode.builder()
                .code(image)
                .value(captcha.getCode())
                .expiredTime(
                        DateUtils.addSeconds(new Date(), CaptchaConstants.IMAGE_VERIFICATION_CODE_EXPIRED_TIME_SECONDS))
                .build();
    }

    /**
     * for unit test
     */
    public VerificationCode generateVerificationCode(Date expiredTime) {
        VerificationCode verificationCode = generateVerificationCode();
        verificationCode.setExpiredTime(
                DateUtils.addSeconds(expiredTime, CaptchaConstants.IMAGE_VERIFICATION_CODE_EXPIRED_TIME_SECONDS));
        return verificationCode;
    }
}
