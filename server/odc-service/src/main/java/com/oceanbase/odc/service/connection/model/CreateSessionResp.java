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
package com.oceanbase.odc.service.connection.model;

import java.util.List;

import com.oceanbase.odc.service.feature.VersionDiffConfigService.OBSupport;
import com.oceanbase.odc.service.feature.model.DataTypeUnit;
import com.oceanbase.odc.service.state.StateIdResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2023/6/5 11:47
 * @Description: []
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionResp implements StateIdResponse {
    private String sessionId;

    private List<DataTypeUnit> dataTypeUnits;

    private List<OBSupport> supports;

    private List<String> charsets;

    private List<String> collations;

    @Override
    public String stateId() {
        return sessionId;
    }
}
