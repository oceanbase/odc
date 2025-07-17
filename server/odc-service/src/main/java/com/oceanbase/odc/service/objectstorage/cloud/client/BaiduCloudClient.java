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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.http.HttpMethodName;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.bos.model.GeneratePresignedUrlRequest;
import com.baidubce.services.bos.model.ResponseHeaderOverrides;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.objectstorage.client.CloudObjectStorageClient;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaiduCloudClient extends AmazonCloudClient {
    private BosClient bosClient;

    public BaiduCloudClient(String ak, String sk, String url, AmazonCloudClient amazonCloudClient) {
        super(amazonCloudClient);
        bosClient = createBosClient(ak, sk, url);
    }

    private BosClient createBosClient(String ak, String sk, String url) {
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(ak, sk));
        String processedURL = processUrl(url);
        log.info("bos init with url={}", processedURL);
        config.setEndpoint(processedURL);
        return new BosClient(config);
    }

    private String processUrl(String originUrl) {
        String url = originUrl.replace("http://", "https://");
        return url.replace("s3.", "");
    }

    @Override
    public URL generatePresignedPutUrl(String bucketName, String key, Date expiration) throws CloudException {
        Verify.notBlank(key, "key");
        return callAmazonMethod("Generate presigned PUT URL", () -> {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            // passed time expire end time, directly use PRESIGNED_UPLOAD_URL_EXPIRATION_SECONDS
            request.setExpiration(CloudObjectStorageClient.PRESIGNED_UPLOAD_URL_EXPIRATION_SECONDS);
            request.setMethod(HttpMethodName.PUT);
            request.setContentType("application/octet-stream");
            return bosClient.generatePresignedUrl(request);
        });
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws CloudException {
        return generatePresignedUrlWithCustomFileName(bucketName, key, expiration, null);
    }

    @Override
    public URL generatePresignedUrlWithCustomFileName(String bucketName, String key, Date expiration,
            String customFileName) throws CloudException {
        Verify.notBlank(key, "key");
        return callAmazonMethod("Generate presigned URL", () -> {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            // passed time expire end time, directly use PRESIGNED_UPLOAD_URL_EXPIRATION_SECONDS
            request.setExpiration(CloudObjectStorageClient.PRESIGNED_UPLOAD_URL_EXPIRATION_SECONDS);
            request.setMethod(HttpMethodName.GET);
            String fileName = customFileName;
            if (StringUtils.isBlank(customFileName)) {
                fileName = CloudObjectStorageUtil.getOriginalFileName(key);
            }
            ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides();
            try {
                responseHeaderOverrides.setContentDisposition(
                        String.format("attachment;filename*=UTF-8''%s",
                                URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            request.setResponseHeaders(responseHeaderOverrides);
            return bosClient.generatePresignedUrl(request);
        });
    }

}
