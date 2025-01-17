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
package com.oceanbase.odc.plugin.connect.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link TestResult}
 *
 * @author yh263208
 * @date 2022-09-29 15:47
 * @since ODC_release_3.5.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class TestResult {

    private boolean active;
    @JsonIgnore
    // @Setter(AccessLevel.NONE)
    private String[] args;
    private ErrorCode errorCode;

    public static TestResult unknownHost(@NonNull String hostName) {
        return fail(ErrorCodes.ConnectionUnknownHost, new String[] {hostName});
    }

    public static TestResult hostUnreachable(@NonNull String hostName) {
        return fail(ErrorCodes.ConnectionHostUnreachable, new String[] {hostName});
    }

    public static TestResult unknownError(Throwable throwable) {
        String message = "Unknown error";
        if (throwable != null) {
            message = throwable.getLocalizedMessage();
        }
        return fail(ErrorCodes.Unknown, new String[] {message});
    }

    public static TestResult unknownPort(@NonNull Integer port) {
        return fail(ErrorCodes.ConnectionUnknownPort, new String[] {port + ""});
    }

    public static TestResult accessDenied(@NonNull String message) {
        return fail(ErrorCodes.ObAccessDenied, new String[] {message});
    }

    public static TestResult unsupportedDBVersion(@NonNull String version) {
        return fail(ErrorCodes.ConnectionUnsupportedDBVersion, new String[] {version});
    }

    public static TestResult obWeakReadConsistencyRequired() {
        return fail(ErrorCodes.ObWeakReadConsistencyRequired, new String[] {});
    }

    public static TestResult initScriptFailed(Throwable throwable) {
        String message = "Unknown error";
        if (throwable != null) {
            message = throwable.getLocalizedMessage();
        }
        return fail(ErrorCodes.ConnectionInitScriptFailed, new String[] {message});
    }

    public static TestResult bucketNotExist(String bucketName) {
        return fail(ErrorCodes.BucketNotExist, new String[] {bucketName});
    }

    public static TestResult invalidAccessKeyId(String accessKeyId) {
        return fail(ErrorCodes.InvalidAccessKeyId, new String[] {accessKeyId});
    }

    public static TestResult akAccessDenied(String accessKeyId) {
        return fail(ErrorCodes.AccessDenied, new String[] {accessKeyId});
    }

    public static TestResult signatureDoesNotMatch(String accessKeyId) {
        return fail(ErrorCodes.SignatureDoesNotMatch, new String[] {accessKeyId});
    }

    public static TestResult success() {
        TestResult result = new TestResult();
        result.setActive(true);
        return result;
    }

    protected static TestResult fail(@NonNull ErrorCode errorCode, @NonNull String[] args) {
        TestResult result = new TestResult();
        result.setErrorCode(errorCode);
        result.args = args;
        return result;
    }

}
