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

package com.oceanbase.odc.plugin.task.api.datatransfer.model;

import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ObjectResult extends ObjectStatus {

    /**
     * for export only, internal usage
     */
    @JsonIgnore
    private String[] exportPaths;

    public static ObjectResult of(ObjectStatus that) {
        ObjectResult result = new ObjectResult();
        result.setType(that.getType());
        result.setStatus(that.getStatus());
        result.setName(that.getName());
        result.setSchema(that.getSchema());
        result.setCount(new AtomicLong(that.getCount().get()));
        result.setTotal(new AtomicLong(that.getTotal().get()));
        return result;
    }

    @JsonIgnore
    public String getSummary() {
        return String.format("%s.%s[%s]", getSchema(), getName(), getType());
    }

}
