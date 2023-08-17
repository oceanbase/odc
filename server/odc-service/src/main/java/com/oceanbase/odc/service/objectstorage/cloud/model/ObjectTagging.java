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
package com.oceanbase.odc.service.objectstorage.cloud.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
public class ObjectTagging {
    private List<Tag> tagSet;

    public static ObjectTagging temp() {
        return new ObjectTagging().withTag("type", "temp");
    }

    public ObjectTagging withTag(String key, String value) {
        if (Objects.isNull(tagSet)) {
            tagSet = new LinkedList<>();
        }
        tagSet.add(new Tag(key, value));
        return this;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Tag {
        private String key;
        private String value;
    }
}
