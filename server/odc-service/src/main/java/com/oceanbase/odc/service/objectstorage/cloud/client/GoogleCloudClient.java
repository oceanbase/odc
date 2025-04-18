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
package com.oceanbase.odc.service.objectstorage.cloud.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/4/14
 */
public class GoogleCloudClient extends AmazonCloudClient {

    public GoogleCloudClient(AmazonS3 s3, AWSSecurityTokenService sts, String roleSessionName, String roleArn) {
        super(s3, sts, roleSessionName, roleArn);
    }

    @Override
    public boolean doesBucketExist(String bucketName) throws CloudException {
        return callAmazonMethod("Check bucket exist", () -> s3.doesBucketExist(bucketName));
    }
}
