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

import org.springframework.beans.BeanUtils;

import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @author jingtian
 */
@NoArgsConstructor
public class OdcDBTableIndex extends DBTableIndex {

    public OdcDBTableIndex(@NonNull DBTableIndex index) {
        BeanUtils.copyProperties(index, this);
    }

    public String getRange() {
        return this.getGlobal() ? "GLOBAL" : "LOCAL";
    }

    public Boolean getPrimaryKey() {
        return this.getPrimary();
    }

    public void setPrimaryKey(Boolean primaryKey) {
        super.setPrimary(primaryKey);
    }

}
