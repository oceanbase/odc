/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.worksheet.model;

/**
 * the request of generating worksheet upload url
 *
 * @author keyang
 * @date 2024/08/08
 * @since 4.3.2
 */
public class GenerateWorksheetUploadUrlResp {
    /**
     * the upload url to upload the file to cloud storage, valid for 1 hour
     */
    String tempUploadUrl;
    /**
     * the object key in cloud storage,to locate uploaded files
     */
    Long objectKey;
}
