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
package com.oceanbase.odc.service.loaddata.util;

import com.oceanbase.odc.service.cloud.model.CloudProvider;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/28
 */
public class CloudProviderUtil {

    public static CloudProvider fromScheme(String scheme) {
        switch (scheme) {
            case "oss":
                return CloudProvider.ALIBABA_CLOUD;
            case "cos":
            case "cosn":
                return CloudProvider.TENCENT_CLOUD;
            case "obs":
                return CloudProvider.HUAWEI_CLOUD;
            case "s3":
            case "s3a":
                return CloudProvider.AWS;
            case "azure":
            case "azblob":
                return CloudProvider.AZURE;
            case "gcs":
            case "gs":
            case "cs":
                return CloudProvider.GOOGLE_CLOUD;
            default:
                throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        }
    }
}
