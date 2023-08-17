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

/**
 * @Author: Lebie
 * @Date: 2022/6/1 下午5:27
 * @Description: []
 */
public class CaptchaConstants {

    public static final String SESSION_KEY_VERIFICATION_CODE = "verificationCode";

    public static final Integer IMAGE_VERIFICATION_CODE_WIDTH = 200;

    public static final Integer IMAGE_VERIFICATION_CODE_HEIGHT = 100;

    public static final Integer IMAGE_VERIFICATION_CODE_DIGITS = 4;

    public static final Integer IMAGE_VERIFICATION_CODE_CIRCLE_COUNT = 20;

    public static final Integer IMAGE_VERIFICATION_CODE_EXPIRED_TIME_SECONDS = 60;
}
