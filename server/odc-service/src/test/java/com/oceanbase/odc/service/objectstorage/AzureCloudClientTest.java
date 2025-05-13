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
package com.oceanbase.odc.service.objectstorage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.validation.constraints.NotBlank;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.objectstorage.cloud.client.AzureCloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.CompleteMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.CopyObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.DeleteObjectsResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.GetObjectRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.InitiateMultipartUploadResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectTagging;
import com.oceanbase.odc.service.objectstorage.cloud.model.PartETag;
import com.oceanbase.odc.service.objectstorage.cloud.model.PutObjectResult;
import com.oceanbase.odc.service.objectstorage.cloud.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartRequest;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadPartResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2025/5/12 11:46
 */
@Slf4j
public class AzureCloudClientTest {

    private static BlobServiceClient serviceClient;
    private AzureCloudClient azureCloudClient;

    static {
        BlobServiceClientBuilder blobServiceClientBuilder = new BlobServiceClientBuilder()
                .connectionString(
                        "your url");
        serviceClient = blobServiceClientBuilder.buildClient();
    }

    @Before
    public void init() {
        azureCloudClient = new AzureCloudClient(serviceClient);
    }

    @Test
    public void testBucketExits() {
        boolean exist = azureCloudClient.doesBucketExist("odccontainer");
        boolean notExist = azureCloudClient.doesBucketExist("ssssss");
        System.out.println("that");
    }

    @Test
    public void testUploadCover() throws IOException {
        String content = "this is odc test";
        String fileName = "uploadFile";
        String blobFileName = "odcblob";
        rmFile(fileName);
        saveContentInLocalFile(fileName, content, BufferedWriter::new);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setETag("that tag");
        ObjectTagging objectTagging = new ObjectTagging();
        objectTagging.withTag("t1", "k1");
        objectTagging.withTag("t2", "k2");
        objectMetadata.setTagging(objectTagging);
        objectMetadata.setContentType("application/octet-stream");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("u1", "v1");
        metadata.put("u2", "v2");
        objectMetadata.setUserMetadata(metadata);
        objectMetadata.setExpirationTime(Date.from(Instant.now()));
        objectMetadata.setContentMD5(DigestUtils.md5Hex(new FileInputStream(fileName)));
        PutObjectResult ret =
                azureCloudClient.putObject("odccontainer", blobFileName, new File(fileName), objectMetadata);
        System.out.println(ret);
        ObjectMetadata saved = azureCloudClient.getObjectMetadata("odccontainer", blobFileName);
        System.out.println(saved);
        GetObjectRequest request = new GetObjectRequest();
        request.setBucketName("odccontainer");
        request.setKey(blobFileName);
        rmFile(fileName + "2");
        azureCloudClient.getObject(request, new File(fileName + "2"));
        System.out.println("done");
    }


    @Test
    public void testUploadAndDownload() throws IOException {
        String content = "this is odc test";
        String fileName = "uploadFile";
        String blobFileName = "blobUploadFile";
        rmFile(fileName);
        saveContentInLocalFile(fileName, content, BufferedWriter::new);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setETag("that tag");
        ObjectTagging objectTagging = new ObjectTagging();
        objectTagging.withTag("t1", "k1");
        objectTagging.withTag("t2", "k2");
        objectMetadata.setTagging(objectTagging);
        objectMetadata.setContentType("application/octet-stream");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("u1", "v1");
        metadata.put("u2", "v2");
        objectMetadata.setUserMetadata(metadata);
        objectMetadata.setExpirationTime(Date.from(Instant.now()));
        objectMetadata.setContentMD5(DigestUtils.md5Hex(new FileInputStream(fileName)));
        PutObjectResult ret =
                azureCloudClient.putObject("odccontainer", blobFileName, new File(fileName), objectMetadata);
        System.out.println(ret);
        ObjectMetadata saved = azureCloudClient.getObjectMetadata("odccontainer", blobFileName);
        System.out.println(saved);
        GetObjectRequest request = new GetObjectRequest();
        request.setBucketName("odccontainer");
        request.setKey(blobFileName);
        azureCloudClient.getObject(request, new File(fileName + "2"));
        System.out.println("done");
    }

    @Test
    public void testUploadAndDownload2() throws IOException {
        String content = "this is odc test";
        String fileName = "uploadFileStream";
        String blobFileName = "blobUploadFileStream";
        rmFile(fileName);
        saveContentInLocalFile(fileName, content, BufferedWriter::new);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setETag("that tag");
        ObjectTagging objectTagging = new ObjectTagging();
        objectTagging.withTag("t1", "k11");
        objectTagging.withTag("t2", "k22");
        objectMetadata.setTagging(objectTagging);
        objectMetadata.setContentType("application/octet-stream");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("u1", "v11");
        metadata.put("u2", "v22");
        objectMetadata.setUserMetadata(metadata);
        objectMetadata.setExpirationTime(Date.from(Instant.now()));
        objectMetadata.setContentMD5(DigestUtils.md5Hex(new FileInputStream(fileName)));
        PutObjectResult ret = azureCloudClient.putObject("odccontainer", blobFileName,
                new FileInputStream(new File(fileName)), objectMetadata);
        System.out.println(ret);
        ObjectMetadata saved = azureCloudClient.getObjectMetadata("odccontainer", blobFileName);
        System.out.println(saved);
        GetObjectRequest request = new GetObjectRequest();
        request.setBucketName("odccontainer");
        request.setKey(blobFileName);
        StorageObject object = azureCloudClient.getObject(request.getBucketName(), request.getKey());
        InputStream stream = object.getObjectContent();
        FileOutputStream fileOutputStream = new FileOutputStream(new File(fileName + "Stream"));
        byte[] arr = new byte[1024];
        int len = -1;
        while ((len = stream.read(arr)) != -1) {
            fileOutputStream.write(arr, 0, len);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
        System.out.println("done");
    }

    @Test
    public void testCopy() {
        String blobFileName = "blobUploadFile";
        String moveName = blobFileName + "Moved";
        CopyObjectResult ret = azureCloudClient.copyObject("odccontainer", blobFileName, moveName);
        System.out.println(ret);
        ObjectMetadata metadata = azureCloudClient.getObjectMetadata("odccontainer", moveName);
        System.out.println(metadata);
    }

    @Test
    public void testDelete() {
        String blobFileName = "blobUploadFile";
        String moveName = blobFileName + "Moved";
        DeleteObjectRequest deleteObjectsResult = new DeleteObjectRequest();
        deleteObjectsResult.setBucketName("odccontainer");
        deleteObjectsResult.setKey(moveName);
        System.out.println(azureCloudClient.doesObjectExist("odccontainer", moveName));
        String ret = azureCloudClient.deleteObject(deleteObjectsResult);
        System.out.println(ret);
        System.out.println(azureCloudClient.doesObjectExist("odccontainer", moveName));
        ObjectMetadata metadata = azureCloudClient.getObjectMetadata("odccontainer", moveName);
        System.out.println(metadata);
    }

    @Test
    public void testDeletes() {
        String blobFileName = "blobUploadFile";
        String moveName = blobFileName + "Moved";
        DeleteObjectsRequest deleteObjectsResult = new DeleteObjectsRequest();
        deleteObjectsResult.setKeys(Arrays.asList(moveName, blobFileName));
        deleteObjectsResult.setBucketName("odccontainer");
        DeleteObjectsResult ret = azureCloudClient.deleteObjects(deleteObjectsResult);
        System.out.println(ret);
        System.out.println(azureCloudClient.doesObjectExist("odccontainer", blobFileName));
        ObjectMetadata metadata = azureCloudClient.getObjectMetadata("odccontainer", blobFileName);
        System.out.println(metadata);
    }

    @Test
    public void testPreSignedURL() {
        String blobFileName = "thatblob";
        URL url = azureCloudClient.generatePresignedUrlWithCustomFileName("odccontainer", blobFileName,
                Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + 3600 * 1000)), "myFile");
        System.out.println(url.toString());
    }

    // curl --request PUT --header "x-ms-blob-type:BlockBlob" --data "@./odcupload"
    // https://odctest2.blob.core.windows.net/odccontainer/uploadblob2\?sv\=2025-01-05\&se\=2025-05-13T04%3A34%3A54Z\&sr\=b\&sp\=cwt\&sig\=0cZtFmb4mOiSjIsm1m%2Fu3zpySaj%2B2cGvLK%2FcK5%2FSKso%3D
    @Test
    public void testPreSignedPutURL() throws IOException {
        String blobFileName = "uploadblob2";
        URL url = azureCloudClient.generatePresignedPutUrl("odccontainer", blobFileName,
                Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + 3600 * 1000)));
        System.out.println(url.toString());
    }

    @Test
    public void testUploadBigFile() throws IOException {
        String fileName = "bigFile";
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setETag("that tag");
        ObjectTagging objectTagging = new ObjectTagging();
        objectTagging.withTag("t1", "k111");
        objectTagging.withTag("t2", "k222");
        objectMetadata.setTagging(objectTagging);
        objectMetadata.setContentType("application/octet-stream");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("u1", "v111");
        metadata.put("u2", "v222");
        objectMetadata.setUserMetadata(metadata);
        objectMetadata.setExpirationTime(Date.from(Instant.now()));
        objectMetadata.setContentMD5(DigestUtils.md5Hex(new FileInputStream(fileName)));
        CompleteMultipartUploadResult ret = multiPartUpload(azureCloudClient, "odccontainer", "bigFileUpload",
                new File(fileName), objectMetadata);
        System.out.println(ret);
        ObjectMetadata metadata2 = azureCloudClient.getObjectMetadata("odccontainer", "bigFileUpload");
        System.out.println(metadata2);
    }


    // delete file
    public static boolean rmFile(String fileName) {
        File f = new File(fileName);
        if (f.exists()) {
            f.delete();
            return true;
        } else {
            return false;
        }
    }


    public static void swallowErrorClose(AutoCloseable closeable) {
        try {
            if (null != closeable) {
                closeable.close();
            }
        } catch (Throwable e) {
        }
    }

    public static void saveContentInLocalFile(String fileName, String content, Function<Writer, Writer> wrapWriter)
            throws IOException {
        String fileNameTmp = fileName + ".tmp";
        File tmp = new File(fileNameTmp);
        if (!tmp.exists()) {
            FileUtils.touch(tmp);
        }
        try (Writer out = wrapWriter.apply(new FileWriter(fileNameTmp, false))) {
            out.write(content);
            out.flush();
        }
        boolean renamed = tmp.renameTo(new File(fileName));
        if (!renamed) {
            log.info("File: \"{}\" rename to \"{}\"", tmp, fileName);
        }
    }


    private CompleteMultipartUploadResult multiPartUpload(AzureCloudClient client, String bucketName,
            @NotBlank String objectName, @NonNull File file,
            ObjectMetadata metadata) throws IOException {
        long fileLength = file.length();
        long partSize = calculatePartSize(fileLength);
        InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                new InitiateMultipartUploadRequest(bucketName, objectName, metadata);
        InitiateMultipartUploadResult initiateMultipartUploadResult =
                client.initiateMultipartUpload(initiateMultipartUploadRequest);
        String uploadId = initiateMultipartUploadResult.getUploadId();
        List<PartETag> partTags = new ArrayList<>();
        int partCount = (int) (fileLength / partSize);
        if (fileLength % partSize != 0) {
            partCount++;
        }
        for (int i = 0; i < partCount; i++) {
            long startPos = i * partSize;
            long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
            try (InputStream input = Files.newInputStream(file.toPath())) {
                long skip = input.skip(startPos);
                Verify.equals(startPos, skip, "skipped size");
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(objectName);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(input);
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber(i + 1);
                UploadPartResult uploadPartResult = client.uploadPart(uploadPartRequest);
                partTags.add(uploadPartResult.getPartETag());
            }
        }
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, partTags, metadata);
        CompleteMultipartUploadResult completeMultipartUploadResult =
                client.completeMultipartUpload(completeMultipartUploadRequest);
        log.info("Complete multipart upload, result={}", completeMultipartUploadResult);
        return completeMultipartUploadResult;
    }

    public long calculatePartSize(long fileLength) {
        long partSize = fileLength / CloudObjectStorageConstants.MAX_PART_COUNT;
        if (fileLength % CloudObjectStorageConstants.MAX_PART_COUNT != 0) {
            partSize += 1;
        }
        if (partSize < CloudObjectStorageConstants.MIN_PART_SIZE) {
            partSize = CloudObjectStorageConstants.MIN_PART_SIZE;
        }
        return partSize;
    }

}
