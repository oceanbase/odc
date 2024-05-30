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
package com.oceanbase.odc.service.queryprofile.model;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.Translatable;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/5/29
 */
public enum ProfileOverviewKey implements Translatable {

    DB_TIME,
    CHANGE_TIME,
    QUEUE_TIME,
    PARALLEL,
    PROCESS_NAME,
    SKEWNESS,
    STATUS,
    PLAN_TYPE,
    IS_HIT_PLAN_CACHE,
    ;

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        return translate(null, LocaleContextHolder.getLocale());
    }
}
