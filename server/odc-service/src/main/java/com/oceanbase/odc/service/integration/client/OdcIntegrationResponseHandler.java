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

package com.oceanbase.odc.service.integration.client;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.util.EntityUtils;

import com.oceanbase.odc.service.integration.model.OdcIntegrationResponse;

/**
 * @author gaoda.xy
 * @date 2023/9/20 11:31
 */
public class OdcIntegrationResponseHandler extends AbstractResponseHandler<OdcIntegrationResponse> {

    public OdcIntegrationResponseHandler() {}

    public OdcIntegrationResponse handleEntity(HttpEntity entity) throws IOException {
        return OdcIntegrationResponse.builder()
                .content(EntityUtils.toString(entity))
                .contentType(ContentType.get(entity))
                .build();
    }

    public OdcIntegrationResponse handleResponse(HttpResponse response) throws HttpResponseException, IOException {
        return super.handleResponse(response);
    }

}
