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
package com.oceanbase.odc.service.db.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2022/6/9
 */

@Data
public class CompileResult {
    private Boolean successful = true;
    @JsonIgnore
    private DBPLObjectIdentity identity;
    private String errorMessage;

    public boolean getStatus() {
        return this.successful;
    }

    public String getTrack() {
        return this.errorMessage;
    }

    public Map<String, String> getPLIdentity() {
        if (this.identity == null) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        map.putIfAbsent("obDbObjectType", identity.getType().name());
        map.putIfAbsent("owner", null);
        map.putIfAbsent("plName", this.identity.getName());
        return map;
    }

}
