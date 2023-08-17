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
package com.oceanbase.odc.core.shared.rest;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.rest.OdcRestTemplate.ODCRestContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class OdcRestLogInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ODCRestContext odcRestContext = OdcRestTemplate.get();
        Verify.notNull(odcRestContext, "odcRestContext");
        odcRestContext.setRealUrl(request.getURI().toString());
        log.info("start call rest {} , method:{} , URL:{}", odcRestContext.getApiName(), request.getMethod(),
                request.getURI());
        return execution.execute(request, body);
    }
}
