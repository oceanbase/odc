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
package com.oceanbase.odc.service.worksheet.model;

import java.util.List;

import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * the request of batch upload project worksheets
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadWorksheetsReq {
    @Size(min = 1, max = 100)
    private List<UploadWorksheetTuple> worksheets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadWorksheetTuple {
        private String path;
        private String objectId;
        private Long size;
    }
}
