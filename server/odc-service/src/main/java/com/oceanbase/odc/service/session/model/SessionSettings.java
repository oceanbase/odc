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
package com.oceanbase.odc.service.session.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * varaibles attached with session
 *
 * @author yh263208
 * @date 2021-04-09 10:28
 * @since ODC_release_2.4.1
 */
@Getter
@Setter
@ToString
public class SessionSettings {
    /**
     * AutoCommit flag, used to illustrate the commit mode
     */
    @NotNull(message = "AutoCommit flag can not be null")
    private Boolean autocommit;
    /**
     * Delimiter, can not be blank
     */
    @NotBlank(message = "delimiter can not be blank")
    private String delimiter;
    private String obVersion;
    /**
     * Query limit settings
     */
    @Min(value = 0, message = "QueryLimit can not be negative")
    @Max(value = Integer.MAX_VALUE, message = "QueryLimit can not be bigger than " + Integer.MAX_VALUE)
    private Integer queryLimit;
}
