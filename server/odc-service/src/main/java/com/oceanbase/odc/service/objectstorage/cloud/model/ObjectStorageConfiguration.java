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

import java.io.Serializable;

import com.aliyuncs.endpoint.EndpointResolver;
import com.aliyuncs.endpoint.LocalConfigRegionalEndpointResolver;
import com.aliyuncs.endpoint.ResolveEndpointRequest;
import com.aliyuncs.exceptions.ClientException;
import com.amazonaws.regions.RegionUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;

import lombok.Data;

@Data
public class ObjectStorageConfiguration implements Serializable {
    /**
     * if cloud storage supports
     */
    private CloudProvider cloudProvider;
    private String region;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * roleArn, roleSessionName are used while generate temporary credential in upload file scenario
     */
    private String roleArn;

    private String roleSessionName;

    /**
     * for aws s3, if endpoint not set, get by region
     */
    public String getEndpoint() {
        if (StringUtils.isBlank(endpoint) && StringUtils.isNotBlank(region)) {
            if (CloudProvider.AWS == cloudProvider) {
                return RegionUtils.getRegion(region).getServiceEndpoint("s3");
            } else if (CloudProvider.ALIBABA_CLOUD == cloudProvider) {
                try {
                    ResolveEndpointRequest request = new ResolveEndpointRequest(region, "oss", null, null);
                    EndpointResolver endpointResolver = new LocalConfigRegionalEndpointResolver();
                    return endpointResolver.resolve(request);
                } catch (ClientException e) {
                    throw new UnexpectedException("getProfile failed with region=" + region, e);
                }
            }
        }
        return this.endpoint;
    }

    public enum CloudProvider {
        NONE("NONE"),
        ALIBABA_CLOUD("ALIBABA_CLOUD", "ALIYUN"),
        AWS("AWS", "AMAZON_WEB_SERVICE"),
        AZURE("AZURE"),
        GOOGLE_CLOUD("GOOGLE_CLOUD", "GCE"),
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
            throw new IllegalArgumentException("CloudProvider value not supported, given value '" + value + "'");
        }

        @JsonValue
        public String getValue() {
            return this.values[0];
        }

        public String[] getValues() {
            return this.values;
        }
    }
}
