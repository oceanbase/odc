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

import java.util.List;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.Range;

import com.oceanbase.odc.service.onlineschemachange.oms.annotation.OmsEnumsCheck;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsOceanBaseType;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
public class ListDataSourceRequest extends BaseOmsRequest {
    /**
     * 数据源ID or 名称
     */
    private String searchKey;
    /**
     * 数据源类型 MYSQL、OB_MYSQL、OB_ORACLE
     */
    @OmsEnumsCheck(fieldName = "types", enumClass = OmsOceanBaseType.class)
    private List<String> types;
    /**
     * 排序字段 gmtCreate(默认)、gmtModified
     */
    private String sortField;
    /**
     * 排序顺序 ascend：升序 descend:降序（默认）
     */
    private String order;
    /**
     * 当前页
     */
    @Min(1)
    private Integer pageNumber;
    /**
     * 每页个数,默认 10
     */
    @Range(min = 1, max = 10)
    private Integer pageSize;

}
