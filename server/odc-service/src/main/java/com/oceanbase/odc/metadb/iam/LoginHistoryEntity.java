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
package com.oceanbase.odc.metadb.iam;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2021/6/28
 */

@Entity
@Table(name = "iam_login_history")
@Data
public class LoginHistoryEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "login_time", nullable = false)
    private OffsetDateTime loginTime;

    @Column(name = "is_success")
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "failed_reason")
    private FailedReason failedReason;

    public enum FailedReason {
        /**
         * user is disable
         */
        USER_NOT_ENABLED,
        /**
         * password incorrect
         */
        BAD_CREDENTIALS,

        /**
         * login user is not found
         */
        USER_NOT_FOUND,

        /**
         * login user is not found or wrong password
         */
        USER_NOT_FOUND_OR_BAD_CREDENTIALS,

        /**
         * too many login attempts with error
         */
        TOO_MANY_ATTEMPTS,

        /**
         * other login result
         */
        OTHER
    }
}
