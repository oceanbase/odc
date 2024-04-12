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
package com.oceanbase.odc.service.queryprofile.model;

import java.util.Map;

import com.oceanbase.odc.core.flow.graph.GraphVertex;
import com.oceanbase.odc.service.queryprofile.model.SqlProfile.Status;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/11
 */
@Getter
@Setter
public class Operator extends GraphVertex {
    private String title;
    private Status status;
    private Map<String, String> overview;
    private Map<String, String> statistics;

    public Operator(@NonNull String id, String name) {
        super(id, name);
    }

    @Override
    public void setAttribute(@NonNull String key, Object value) {
        if (value != null) {
            super.setAttribute(key, value);
        }
    }
}
