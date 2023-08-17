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
package com.oceanbase.odc.core.flow.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link BaseGraphElement}
 *
 * @author yh263208
 * @date 2022-01-14 16:27
 * @since ODC_release_3.3.0
 */
@Getter
@ToString
@EqualsAndHashCode(exclude = {"name", "attributes"})
public abstract class BaseGraphElement {
    private final String graphId;
    @Setter
    private String name;
    private final Map<String, Object> attributes;

    public BaseGraphElement(@NonNull String graphId, String name) {
        this.name = name;
        this.graphId = graphId;
        this.attributes = new HashMap<>();
    }

    public void setAttribute(@NonNull String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(@NonNull String key) {
        return this.attributes.get(key);
    }

    public Set<String> getAttributeKeys() {
        return this.attributes.keySet();
    }

    public Object removeAttribute(@NonNull String key) {
        return this.attributes.remove(key);
    }

}
