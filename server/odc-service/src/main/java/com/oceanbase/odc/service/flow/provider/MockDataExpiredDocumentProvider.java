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
package com.oceanbase.odc.service.flow.provider;

import java.io.File;

import com.oceanbase.odc.service.flow.task.model.MockProperties;

import lombok.NonNull;

/**
 * {@link BaseExpiredDocumentProvider} for {@code Mock Task}
 *
 * @author yh263208
 * @date 2022-03-31 21:05
 * @since ODC_reelase_3.3.0
 */
public class MockDataExpiredDocumentProvider extends BaseExpiredDocumentProvider {

    private final MockProperties mockProperties;

    public MockDataExpiredDocumentProvider(@NonNull int fileExpireHours, @NonNull MockProperties mockProperties) {
        super(fileExpireHours);
        this.mockProperties = mockProperties;
    }

    @Override
    protected File getRootScanDir() {
        return new File(mockProperties.getResultFileLocationPrefix());
    }
}
