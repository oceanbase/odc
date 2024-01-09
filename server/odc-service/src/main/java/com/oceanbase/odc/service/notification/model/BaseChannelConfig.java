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

package com.oceanbase.odc.service.notification.model;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/4
 */
@Data
public class BaseChannelConfig {

    private Locale language;

    private String titleTemplate;

    private String contentTemplate;

    private RateLimitConfig rateLimitConfig;

    @JsonSetter("language")
    public void setLanguage(String language) {
        try {
            this.language = Locale.forLanguageTag(language);
        } catch (Exception e) {
            this.language = LocaleContextHolder.getLocale();
        }
    }

}
