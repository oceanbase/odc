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
package com.oceanbase.odc.service.connection.model;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.Validate;

import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response for <code>ConnectionPermissionChecker</code>
 *
 * @author yh263208
 * @date 2021-09-02 15:45
 * @since ODC_release_3.2.0
 */
@Getter
@ToString
public class ConnectionPermissionCheckResp {
    @Setter
    private Boolean allCheckPass;
    private final List<Message> messages = new LinkedList<>();

    @Data
    public static class Message {
        private String accountType;
        private String accountName;
        private String message;

        public Message(ConnectionAccountType accountType, String accountName, ErrorCodes errorCodes, Object[] params) {
            Validate.notNull(accountName, "AccountName can not be null for Message");
            Validate.notNull(accountType, "AccountType can not be null for Message");
            Validate.notNull(errorCodes, "ErrorCode can not be null for Message");
            Validate.notNull(params, "Params can not be null for Message");
            this.accountType = accountType.getLocalizedMessage();
            this.accountName = accountName;
            this.message = errorCodes.getLocalizedMessage(params);
        }
    }

}


