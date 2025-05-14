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

import com.aliyuncs.endpoint.EndpointResolver;
import com.aliyuncs.endpoint.LocalConfigRegionalEndpointResolver;
import com.aliyuncs.endpoint.ResolveEndpointRequest;
import com.aliyuncs.exceptions.ClientException;
import com.amazonaws.regions.RegionUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.loaddata.model.ObjectStorageConfig;

import lombok.Data;

@Data
public class ObjectStorageConfiguration {
    /**
     * if cloud storage supports
     */
    private CloudProvider cloudProvider;
    private String region;
    private String publicEndpoint;
    private String internalEndpoint;
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
    public String getPublicEndpoint() {
        // return if set
        if (!StringUtils.isBlank(publicEndpoint)) {
            return publicEndpoint;
        }
        if (CloudProvider.AWS == cloudProvider && StringUtils.isNotBlank(region)) {
            return RegionUtils.getRegion(region).getServiceEndpoint("s3");
        } else if (CloudProvider.ALIBABA_CLOUD == cloudProvider && StringUtils.isNotBlank(region)) {
            try {
                ResolveEndpointRequest request = new ResolveEndpointRequest(region, "oss", null, null);
                EndpointResolver endpointResolver = new LocalConfigRegionalEndpointResolver();
                return endpointResolver.resolve(request);
            } catch (ClientException e) {
                throw new UnexpectedException("getProfile failed with region=" + region, e);
            }
        } else if (CloudProvider.AZURE == cloudProvider) {
            // for azure compute as it's endpoint
            return ObjectStorageConfig.concatEndpoint(cloudProvider, accessKeySecret);
        }
        return publicEndpoint;
    }

    public String getInternalEndpoint() {
        if (StringUtils.isBlank(internalEndpoint) || StringUtils.contains(internalEndpoint, "CHANGE_ME")) {
            return getPublicEndpoint();
        }
        return this.internalEndpoint;
    }

}
