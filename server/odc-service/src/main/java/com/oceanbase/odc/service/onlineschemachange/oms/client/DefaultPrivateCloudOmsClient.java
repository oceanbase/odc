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
package com.oceanbase.odc.service.onlineschemachange.oms.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Component
@Slf4j
public class DefaultPrivateCloudOmsClient extends BaseOmsClient {

    private final OnlineSchemaChangeProperties onlineSchemaChangeProperties;

    public DefaultPrivateCloudOmsClient(
            @Autowired OnlineSchemaChangeProperties onlineSchemaChangeProperties,
            @Autowired RestTemplate omsRestTemplate) {
        super(onlineSchemaChangeProperties.getOms().getUrl() + "/api/v2", omsRestTemplate);
        this.onlineSchemaChangeProperties = onlineSchemaChangeProperties;
    }

    @Override
    protected void setHttpHeaders(HttpHeaders httpHeaders) {
        httpHeaders.setBasicAuth(onlineSchemaChangeProperties.getOms().getAuthorization());
    }

}
