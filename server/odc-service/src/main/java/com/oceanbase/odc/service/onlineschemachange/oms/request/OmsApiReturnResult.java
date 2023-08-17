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
package com.oceanbase.odc.service.onlineschemachange.oms.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * oms 接口统一返回值
 *
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Setter
@Getter
@ToString
@JsonFormat(with = Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class OmsApiReturnResult<T> implements ApiReturnResult<T> {

    /**
     * 是否调用成功
     */
    private boolean success;

    /**
     * 错误详情
     */
    private String errorDetail;

    /**
     * result code
     */
    private String code;

    /**
     * 描述
     */
    private String message;

    /**
     * 建议
     */
    private String advice;
    /**
     * requestId
     */
    private String requestId;
    /**
     * pageNumber
     */
    private Integer pageNumber;
    /**
     * pageSize
     */
    private Integer pageSize;
    /**
     * totalCount
     */
    private Long totalCount;

    /**
     * 请求耗时
     */
    private String cost;
    /**
     * 数据
     */
    private T data;

    /**
     * ocp return
     */
    private Boolean deletable;

    /**
     * ocp return
     */
    private String errorCode;

    /**
     * ocp return
     */
    private String trace;

}
