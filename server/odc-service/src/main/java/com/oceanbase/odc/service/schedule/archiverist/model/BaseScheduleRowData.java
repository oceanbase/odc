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
package com.oceanbase.odc.service.schedule.archiverist.model;

import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.service.exporter.model.Encryptable;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

import lombok.Data;

@Data
public class BaseScheduleRowData implements Encryptable {

    private String rowId = UUID.randomUUID().toString();

    @NotBlank
    private String name;

    @NotNull
    private ScheduleType type;

    @NotNull
    private TriggerConfig triggerConfig;

    private String description;

    @Override
    public void encrypt(String encryptKey) {}

    @Override
    public void decrypt(String encryptKey) {}
}
