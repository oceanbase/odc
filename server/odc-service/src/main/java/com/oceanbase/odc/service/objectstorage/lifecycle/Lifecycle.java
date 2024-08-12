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
package com.oceanbase.odc.service.objectstorage.lifecycle;

import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * the lifecycle of object storeage
 *
 * @author keyang
 * @date 2024/08/09
 * @since 4.3.2
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Lifecycle {
    private Strategy strategy;
    private Integer expirationDays;

    public static Lifecycle permanent() {
        return Lifecycle.builder().strategy(Strategy.PERMANENT).build();
    }

    public static Lifecycle temp() {
        return Lifecycle.builder().strategy(Strategy.TEMP).build();
    }

    public static Lifecycle expiredAfterLastModified(int days) {
        return Lifecycle.builder().strategy(Strategy.EXPIRED_AFTER_LAST_MODIFIED).expirationDays(days).build();
    }

    public ObjectMetadata toObjectMetadata() {
        ObjectMetadata objectMetadata;
        switch (strategy) {
            case TEMP:
                objectMetadata = ObjectMetadata.temp();
                break;
            default:
                objectMetadata = new ObjectMetadata();
                break;
        }
        objectMetadata.setLifecycle(this);
        return objectMetadata;
    }
}
