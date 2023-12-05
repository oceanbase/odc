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

import java.util.List;

import com.oceanbase.tools.dbbrowser.model.DBVariable;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OdcDBVariable extends DBVariable {
    @ApiModelProperty(value = "value的类型，支持string、numeric、enum三种")
    private String valueType;
    private List<String> valueEnums;
    private String unit;
    private String variableScope;
    private boolean changed = false;

    public String getKey() {
        return this.getName();
    }

    public void setKey(String key) {
        this.setName(key);
    }
}
