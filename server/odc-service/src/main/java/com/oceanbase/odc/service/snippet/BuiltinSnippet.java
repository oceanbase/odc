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
package com.oceanbase.odc.service.snippet;

import java.util.Set;

import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BuiltinSnippet extends Snippet {
    private String name;
    private DialectType dialectType;
    private Set<String> tags;
    private String minVersion;
    private String maxVersion;

    public BuiltinSnippet copy() {
        BuiltinSnippet snippet = new BuiltinSnippet();
        snippet.setName(this.name);
        snippet.setDialectType(this.dialectType);
        snippet.setTags(this.tags);
        snippet.setMinVersion(this.minVersion);
        snippet.setMaxVersion(this.maxVersion);
        snippet.setBody(this.getBody());
        snippet.setDescription(this.getDescription());
        snippet.setId(this.getId());
        snippet.setPrefix(this.getPrefix());
        snippet.setType(this.getType());
        return snippet;
    }
}
