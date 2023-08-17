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
package com.oceanbase.odc.core.migrate.resource.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.migrate.resource.Verifiable;
import com.oceanbase.odc.core.shared.exception.VerifyException;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link TableTemplate}
 *
 * @author yh263208
 * @date 2022-04-20 15:28
 * @since ODC_release_3.3.1
 */
@Getter
@Setter
@ToString
public class TableTemplate implements Verifiable {
    @Valid
    @NotNull
    private TableMetaData metadata;
    private boolean ignore = false;
    @Valid
    @NotNull
    private List<TableSpec> specs;

    @Override
    public void verify() throws VerifyException {
        Set<String> names = new HashSet<>();
        specs.forEach(entity -> {
            entity.verify();
            String name = entity.getName();
            if (!names.contains(name)) {
                names.add(name);
                return;
            }
            throw new VerifyException("Duplicated name: " + name);
        });
    }

}
