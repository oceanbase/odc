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
package com.oceanbase.odc.core.shared.constant;

/**
 * no numerical value used here, as users are not expect to understand digital error code
 * 
 * @author yizhou.xw
 * @version : ErrorCodes.java, v 0.1 2021-02-19 12:39
 */
public enum ErrorCodes implements ErrorCode {

    /**
     * common errors
     */
    Success,
    IllegalArgument,
    ArgumentValueAndTypeMismatched,
    DuplicatedExists,
    ReservedName,
    BadRequest,
    RequestFormatVersionNotMatch,
    NotFound,
    AccessDenied,
    DatabaseAccessDenied,
    LoginExpired,
    UnauthorizedSessionAccess,
    PermissionChanged,
    UnauthorizedDataAccess,
    OverLimit,
    TooManyRequest,
    Unexpected,
    Unsupported,
    NotImplemented,
    DataAccessError,
    InternalServerError,
    TooManyResultSetsToBeCached,
    DataTooLarge,
    CannotOperateDueReference,
    Timeout,
    Unknown,
    InvalidFileFormat,
    IllegalFileName,

    /**
     * common argument validation
     */
    BadArgument,
    NotNull,
    NotEmptyString,
    NotBlankString,
    NotEmptyList,
    NotNegative,
    SyntaxError,
    FieldUpdateNotSupported,

    /**
     * odc modules
     */
    // Auth
    AuthNoToken,
    AuthInvalidToken,
    MissingCsrfToken,
    InvalidCsrfToken,
    IllegalOperation,
    WrongVerificationCode,
    ExpiredVerificationCode,
    MissingVerificationCode,
    BlankVerificationCode,
    AttemptLoginOverLimit,

    // User
    UserWrongPasswordOrNotFound,
    UserIllegalNewPassword,
    UserInvalidPassword,
    UserNotActive,
    UserNotEnabled,

    // Config

    // Connection
    ConnectionReset,
    ConnectionDuplicatedName,
    ConnectionInvalidSidFormat,
    ConnectionUnsupportedType,
    ConnectionInvalidClientCommandLine,
    ConnectionExpired,
    ConnectionPasswordMissed,
    ConnectionNotEnabled,
    SysTenantAccountNotSet,
    SysTenantAccountInvalid,
    ConnectionVerifyFailed,
    ConnectionDatabaseTypeMismatched,
    ConnectionInitScriptFailed,
    ConnectionInsufficientPermissions,
    ConnectionTooManyPermissions,
    ConnectionReadonly,
    ConnectionOccupied,
    ConnectHostNotAllowed,
    ConnectionTempOnly,
    ConnectionFlowConfigNotExists,
    LogicalTableBadExpressionSyntax,
    LogicalTableExpressionNotEvenlyDivided,
    LogicalTableExpressionNotPositiveStep,
    LogicalTableExpressionRangeStartGreaterThanEnd,
    LogicalTableExpressionNotValidIntegerRange,

    // File
    FileWriteFailed,
    FileCreateUnauthorized,
    FilePathTraversalDetected,
    FileUploading,
    FileSuffixNotAllowed,

    // Task
    RunningTaskNotTerminable,
    FinishedTaskNotTerminable,
    TaskNotReadyForDownload,
    TaskSqlExecuteFailed,
    TaskLogNotFound,

    // Debug
    DebugDBMSNotSupported,
    DebugDBMSCallFailed,
    DebugPackageCreateFailed,
    DebugTypeUnsupported,
    DebugObjectUnsupported,
    DebugSessionNotAlive,
    DebugStartFailed,
    DebugInfoParseFailed,
    DebugTimeout,

    // PL
    PLObjectFetchFailed,
    PLDebugKernelUnknownError,
    ExecPLByAnonymousBlockUndefinedParameters,
    ExecPLByAnonymousBlockErrorFormatParameters,
    ExecPLByAnonymousBlockDifferentParameters,
    ExecPLByAnonymousBlockErrorCallStatement,

    // Mock
    MockError,

    // load/dump
    ImportInvalidZip,
    ImportInvalidFileType,

    // Flow
    FlowTaskInstanceExpired,
    FlowTaskInstanceFailed,
    FlowTaskInstanceCancelled,
    FlowCreateDeniedBySqlCheck,

    // Schedule
    AlterScheduleExists,
    InvalidCronExpression,

    // Partition plan
    PartitionPlanNoDropPreviewSqlGenerated,
    PartitionPlanNoCreatePreviewSqlGenerated,
    InvalidSqlExpression,
    PartitionKeyDataTypeMismatch,
    TimeDataTypePrecisionMismatch,

    // Import & Export
    ExportExcelFileFailed,

    // Online Schema Change
    OscSqlTypeInconsistent,
    NoUniqueKeyExists,
    OscNotEnabled,
    OscLockUserRequired,
    OscUnsupportedForeignKeyTable,
    OscColumnNameInconsistent,
    OscAddPrimaryKeyColumnNotAllowed,
    OscDataCheckInconsistent,
    OscSwapTableStarted,
    OmsBindTargetNotFound,
    OmsDataCheckInconsistent,
    OmsGhanaOperateFailed,
    OmsParamError,
    OmsConnectivityTestFailed,
    OmsPreCheckFailed,
    OmsProjectExecutingFailed,

    // resource
    BuiltInResourceOperateNotAllowed,
    BuiltInResourceNotAvailable,
    ResourceCreating,
    ResourceModifying,
    ResourceSynchronizing,

    // Integration
    EnableSqlInterceptorNotAllowed,

    /**
     * external service error
     */
    ExternalServiceError,
    ExternalAasServiceError,
    ExternalOAMServiceError,
    ExternalRAMServiceError,
    ExternalOssError,
    ExternalVpcError,
    ExternalUrlNotAllowed,

    /**
     * ob operation
     */
    ObConnectFailed,
    ObFeatureNotSupported,
    ObExecutePlFailed,
    ObExecuteSqlFailed,
    ObExecuteSqlSocketTimeout,
    ObExecuteSqlTimeout,
    ObExecuteSqlCanceled,
    ObInvalidCreateSqlCondition,
    ObFileObjectNotFound,
    ObDbObjectNotFound,

    ObInvalidPartType,
    ObPartTypeNotSupported,
    ObBasicTypeColumnRequired,
    ObUpdateKeyRequired,
    ObProcedureExecuteRequireValueForInParam,
    ObAccessDenied,
    ObMysqlAccessDenied,
    ObCommandDenied,
    ObGlobalVariableSetSessionScopeNotSupported,

    ObCreatePlDebugPackageFailed,
    ObCreatePlRunPackageFailed,

    ObGetExecuteDetailFailed,
    ObGetPlanExplainFailed,
    ObGetPlanExplainEmpty,
    ObGetFullLinkTraceFailed,
    ObFullLinkTraceNotSupported,
    ObFullLinkTraceNotEnabled,

    ObPreCheckDdlFailed,
    ObCopySchemaFailed,
    ObInvalidObjectTypesForRecyclebin,

    /**
     * TODO: ob sql validate, mysql mode
     */

    /**
     * ob sql validate, oracle mode
     */
    OracleBlankColumnDefaultValue,

    /**
     * SQL Execute
     */
    SqlInterceptApprovalRequired,
    SqlInterceptBlocked,
    SqlRegulationRuleBlocked,
    SqlInterceptExternalServiceError,

    /**
     * connection test
     */
    ConnectionUnknownHost,
    ConnectionHostUnreachable,
    ConnectionUnknownPort,
    ConnectionUnsupportedConnectType,
    ConnectionUnsupportedDBVersion,

    InsufficientPrivilege,


    /**
     * DB Object
     */
    DBObjectMetadataMayNotAccurate,
    QueryDBVersionFailed,

    /**
     * Permission management
     */
    GrantPermissionFailed;


    @Override
    public String code() {
        return this.name();
    }
}
