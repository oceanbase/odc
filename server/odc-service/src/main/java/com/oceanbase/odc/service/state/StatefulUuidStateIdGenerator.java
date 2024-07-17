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
package com.oceanbase.odc.service.state;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.session.factory.StateHostGenerator;
import com.oceanbase.odc.service.state.model.StatefulUuidStateId;

@Component
public class StatefulUuidStateIdGenerator {

    @Autowired
    private StateHostGenerator stateHostGenerator;

    public String generateStateId(String type) {
        StatefulUuidStateId uuidStateId = StatefulUuidStateId.createTypeUuidStateId(type, StringUtils.uuidNoHyphen(),
                stateHostGenerator.getHost());
        return Base64.getEncoder().encodeToString(JsonUtils.toJson(uuidStateId).getBytes(StandardCharsets.UTF_8));
    }

    public static StatefulUuidStateId parseStateId(String stateId) {
        return JsonUtils.fromJson(new String(Base64.getDecoder().decode(stateId)),
                StatefulUuidStateId.class);
    }

}
