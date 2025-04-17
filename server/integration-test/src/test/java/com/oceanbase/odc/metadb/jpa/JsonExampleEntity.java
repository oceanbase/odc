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
package com.oceanbase.odc.metadb.jpa;

import java.util.List;

import org.hibernate.annotations.Type;

import com.oceanbase.odc.config.jpa.type.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "json_example_entity")
class JsonExampleEntity {

    @Id
    private Long id;

    @Type(value = JsonType.class)
    @Column(name = "json_example")
    private JsonExampleNested jsonExample;

    @Type(value = JsonType.class)
    @Column(name = "json_array")
    private List<List<String>> jsonArray;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class JsonExampleNested {
        private String nestedField1;
        private int nestedField2;
    }
}
