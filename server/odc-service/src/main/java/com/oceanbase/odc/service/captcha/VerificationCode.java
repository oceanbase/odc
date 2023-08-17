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

import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/5/31 下午4:44
 * @Description: []
 */
@Data
@Builder
public class VerificationCode implements Serializable {
    private static final long serialVersionUID = -2612029740072123220L;

    @JsonIgnore
    private transient InputStream code;

    private String value;

    private Date expiredTime;

    public boolean isExpired() {
        return expiredTime.before(new Date());
    }
}
