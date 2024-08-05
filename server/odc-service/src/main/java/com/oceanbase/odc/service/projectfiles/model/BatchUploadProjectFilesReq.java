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
package com.oceanbase.odc.service.projectfiles.model;

import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.Size;

import lombok.Data;

/**
 * 批量上传文件请求
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@Data
public class BatchUploadProjectFilesReq {
    @Size(min = 1, max = 100)
    private Set<UploadProjectFileTuple> files;

    @Data
    public static class UploadProjectFileTuple {
        private String path;
        private String objectKey;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UploadProjectFileTuple that = (UploadProjectFileTuple) o;
            return Objects.equals(path, that.path) && Objects.equals(objectKey, that.objectKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, objectKey);
        }

        @Override
        public String toString() {
            return "UploadProjectFileTuple{" +
                    "path='" + path + '\'' +
                    ", objectKey='" + objectKey + '\'' +
                    '}';
        }
    }
}
