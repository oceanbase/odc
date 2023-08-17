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
package com.oceanbase.odc.service.onlineschemachange.oms.openapi;

import java.util.List;

import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.DataSourceResponse;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
public interface DataSourceOpenApiService {

    /**
     * 创建 OB 数据源
     * 
     * @param request
     * @return
     */
    String createOceanBaseDataSource(CreateOceanBaseDataSourceRequest request);

    /**
     * 查询数据源列表
     * 
     * @param request
     * @return
     */
    List<DataSourceResponse> listDataSource(ListDataSourceRequest request);
}
