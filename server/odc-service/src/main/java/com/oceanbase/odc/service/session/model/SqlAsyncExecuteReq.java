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
package com.oceanbase.odc.service.session.model;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * Request for sql execute
 *
 * @author yh263208
 * @date 2021-11-18 10:52
 * @since ODC_release_3.2.2
 */
@Data
public class SqlAsyncExecuteReq {
    @NotNull
    private String sql;
    private Integer queryLimit;
    private Boolean autoCommit;
    private Boolean split;
    /**
     * oracle mode, if should add ROWID, true by default
     */
    private Boolean addROWID;
    private Boolean showTableColumnInfo;

    public boolean ifSplitSqls() {
        if (this.split == null) {
            return true;
        }
        return this.split;
    }

    public boolean ifAddROWID() {
        if (this.addROWID == null) {
            return true;
        }
        return this.addROWID;
    }
}
