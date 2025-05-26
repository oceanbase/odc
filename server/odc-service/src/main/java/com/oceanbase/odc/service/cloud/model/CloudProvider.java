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
package com.oceanbase.odc.service.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oceanbase.odc.common.util.StringUtils;

public enum CloudProvider {
    NONE("NONE"),
    ALIBABA_CLOUD("ALIBABA_CLOUD", "ALIYUN"),
    AWS("AWS", "AMAZON_WEB_SERVICE"),
    AWSCN("AWSCN", "AMAZON_WEB_SERVICE_CN"),
    AZURE("AZURE"),
    GOOGLE_CLOUD("GOOGLE_CLOUD", "GCP"),
    HUAWEI_CLOUD("HUAWEI_CLOUD", "HUAWEI"),
    TENCENT_CLOUD("TENCENT_CLOUD", "QCLOUD", "TENCENT"),
    BAIDU_CLOUD("BAIDU_CLOUD", "BAIDU"),
    UNKNOWN("UNKNOWN"),
    ;

    private final String[] values;

    CloudProvider(String... values) {
        this.values = values;
    }

    @JsonCreator
    public static CloudProvider fromValue(String value) {
        for (CloudProvider cloudProvider : CloudProvider.values()) {
            for (String optionalValue : cloudProvider.getValues()) {
                if (StringUtils.equalsIgnoreCase(optionalValue, value)) {
                    return cloudProvider;
                }
            }
        }
        return UNKNOWN;
    }

    @JsonValue
    public String getValue() {
        return this.values[0];
    }

    public String[] getValues() {
        return this.values;
    }

    public boolean isAWS() {
        return this == AWS || this == AWSCN;
    }

}
