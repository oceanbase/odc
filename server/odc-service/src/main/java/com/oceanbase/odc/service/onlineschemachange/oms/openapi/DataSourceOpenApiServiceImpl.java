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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.service.onlineschemachange.oms.client.ClientRequestParams;
import com.oceanbase.odc.service.onlineschemachange.oms.client.OmsClient;
import com.oceanbase.odc.service.onlineschemachange.oms.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;
import com.oceanbase.odc.service.onlineschemachange.oms.response.DataSourceResponse;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Service
public class DataSourceOpenApiServiceImpl implements DataSourceOpenApiService {
    private final OmsClient omsClient;

    public DataSourceOpenApiServiceImpl(@Autowired OmsClient omsClient) {
        this.omsClient = omsClient;
    }

    @Override
    public String createOceanBaseDataSource(CreateOceanBaseDataSourceRequest request) {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("CreateOceanBaseDataSource")
                .setTypeReference(new TypeReference<OmsApiReturnResult<String>>() {});

        return omsClient.postOmsInterface(params);
    }

    @Override
    public List<DataSourceResponse> listDataSource(ListDataSourceRequest request)
            throws OmsException {
        ClientRequestParams params = new ClientRequestParams()
                .setRequest(request)
                .setAction("ListDataSource")
                .setTypeReference(new TypeReference<OmsApiReturnResult<List<DataSourceResponse>>>() {});

        return omsClient.postOmsInterface(params);
    }
}
