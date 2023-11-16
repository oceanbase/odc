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
package com.oceanbase.odc.service.session.factory;

import java.util.Base64;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSessionIdGenerator;
import com.oceanbase.odc.service.connection.model.CreateSessionReq;

import lombok.NonNull;

public class DefaultConnectSessionIdGenerator implements ConnectionSessionIdGenerator<CreateSessionReq> {

    private final String fixRealId;

    public DefaultConnectSessionIdGenerator(String fixRealId) {
        this.fixRealId = StringUtils.isBlank(fixRealId) ? null : fixRealId;
    }

    @Override
    public String generateId(CreateSessionReq key) {
        key.setRealId(RandomStringUtils.random(10, UUID.randomUUID().toString().replace("-", "")));
        if (fixRealId != null) {
            key.setRealId(fixRealId);
        }
        return Base64.getEncoder().encodeToString(JsonUtils.toJson(key).getBytes());
    }

    @Override
    public CreateSessionReq getKeyFromId(@NonNull String id) {
        return JsonUtils.fromJson(new String(Base64.getDecoder().decode(id)), CreateSessionReq.class);
    }

}
