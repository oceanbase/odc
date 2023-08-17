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

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.service.onlineschemachange.oms.request.BaseOmsRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;

/**
 * @author yaobin
 * @date 2023-06-06
 * @since 4.2.0
 */
public class ClientRequestParams {
    private BaseOmsRequest request;

    private String action;

    private TypeReference<?> typeReference;

    public BaseOmsRequest getRequest() {
        return request;
    }

    public ClientRequestParams setRequest(BaseOmsRequest request) {
        this.request = request;
        return this;
    }

    public String getAction() {
        return action;
    }

    public ClientRequestParams setAction(String action) {
        this.action = action;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <E> TypeReference<OmsApiReturnResult<E>> getTypeReference() {
        return (TypeReference<OmsApiReturnResult<E>>) typeReference;
    }

    public <E> ClientRequestParams setTypeReference(TypeReference<OmsApiReturnResult<E>> typeReference) {
        this.typeReference = typeReference;
        return this;
    }
}
