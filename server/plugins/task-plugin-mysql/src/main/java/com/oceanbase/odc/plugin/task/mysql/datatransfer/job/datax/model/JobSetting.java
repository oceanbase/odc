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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model;

import lombok.Data;

@Data
public class JobSetting {

    private Speed speed = new Speed();
    private ErrorLimit errorLimit = new ErrorLimit();

    @Data
    public static class ErrorLimit {
        private Long record;
    }

    @Data
    public static class Speed {

        private int channel = 3;

        private int bytes = -1;

    }

}
