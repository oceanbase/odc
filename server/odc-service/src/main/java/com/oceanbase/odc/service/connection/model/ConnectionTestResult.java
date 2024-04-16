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

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.plugin.connect.api.TestResult;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link ConnectionTestResult}
 *
 * @author yh263208
 * @date 2022-09-29 15:47
 * @since ODC_release_3.5.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class ConnectionTestResult extends TestResult {

    private final ConnectType type;

    public ConnectionTestResult(TestResult testResult, ConnectType type) {
        this.setActive(testResult.isActive());
        this.setErrorCode(testResult.getErrorCode());
        this.setArgs(testResult.getArgs());
        this.type = type;
    }

    public static ConnectionTestResult success(ConnectType type) {
        return new ConnectionTestResult(TestResult.success(), type);
    }

    public static ConnectionTestResult fail(@NonNull ErrorCode errorCode, @NonNull String[] args) {
        return new ConnectionTestResult(TestResult.fail(errorCode, args), null);
    }

    public static ConnectionTestResult connectTypeMismatch(@NonNull ConnectType inputType) {
        return fail(ErrorCodes.ConnectionDatabaseTypeMismatched, new String[] {inputType.name()});
    }

    public static ConnectionTestResult unsupportedConnectType(@NonNull ConnectType unsupportedType) {
        return fail(ErrorCodes.ConnectionUnsupportedConnectType, new String[] {unsupportedType.name()});
    }

    public static ConnectionTestResult unknownError(Throwable throwable) {
        return new ConnectionTestResult(TestResult.unknownError(throwable), null);
    }

    public static ConnectionTestResult initScriptFailed(String[] args) {
        if (args != null) {
            return fail(ErrorCodes.ConnectionInitScriptFailed, args);
        }
        return fail(ErrorCodes.ConnectionInitScriptFailed, new String[] {"Unknown error"});
    }

    public static ConnectionTestResult connectTypeMismatch() {
        String args = ConnectType.CLOUD_OB_MYSQL.name() + "/" + ConnectType.CLOUD_OB_ORACLE.name();
        return fail(ErrorCodes.ConnectionDatabaseTypeMismatched, new String[] {args});
    }

    public String getErrorMessage() {
        if (getErrorCode() == null) {
            return null;
        }
        return getErrorCode().getLocalizedMessage(getArgs());
    }

}
