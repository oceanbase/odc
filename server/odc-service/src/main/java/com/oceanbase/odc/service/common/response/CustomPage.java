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
package com.oceanbase.odc.service.common.response;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 分页信息
 */
@ApiModel(description = "分页信息")
@Data
public class CustomPage {

    /**
     * 总记录数
     */
    @JsonProperty("totalElements")
    @ApiModelProperty(example = "100", value = "总记录数")
    private Long totalElements = null;

    /**
     * 总页数
     */
    @ApiModelProperty(example = "100", value = "总页数")
    @JsonProperty("totalPages")
    private Integer totalPages = null;

    /**
     * 当前页码
     */
    @ApiModelProperty(example = "1", value = "当前页码")
    @JsonProperty("number")
    private Integer number = null;

    /**
     * 当前页包含的记录条数
     */
    @ApiModelProperty(example = "10", value = "当前页包含的记录条数")
    @JsonProperty("size")
    private Integer size = null;

    public static CustomPage from(Page<?> page) {
        CustomPage customPage = new CustomPage();
        customPage.setTotalElements(page.getTotalElements());
        customPage.setTotalPages(page.getTotalPages());
        customPage.setNumber(page.getNumber() + 1);
        customPage.setSize(page.getSize());
        return customPage;
    }

    public static CustomPage empty() {
        CustomPage customPage = new CustomPage();
        customPage.setTotalElements(0L);
        customPage.setTotalPages(0);
        customPage.setNumber(0);
        customPage.setSize(0);
        return customPage;
    }

}
