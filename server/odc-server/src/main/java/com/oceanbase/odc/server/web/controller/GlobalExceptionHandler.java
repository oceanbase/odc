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
package com.oceanbase.odc.server.web.controller;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.LogUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.HttpException;
import com.oceanbase.odc.service.common.exception.OdcUncheckedException;
import com.oceanbase.odc.service.common.response.OdcErrorResult;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

import lombok.extern.log4j.Log4j2;

/**
 * @author yixun
 * @version 2.0.1
 * @description 全局异常处理
 * @date 2019-05-06 11:16
 * @since 2.0.1
 */
@ControllerAdvice(basePackages = "com.oceanbase.odc.server.web.controller.v1")
@Log4j2
public class GlobalExceptionHandler {
    private static final String server = SystemUtils.getHostName();

    @Autowired
    private HttpServletRequest request;

    @ExceptionHandler(value = HttpException.class)
    @ResponseBody
    public ResponseEntity<OdcErrorResult> handleHttpException(HttpException e, HandlerMethod handlerMethod) {
        OdcErrorResult result = OdcErrorResult.error(e);
        attachTraceInfo(result);
        logTraceInfo(result, e, handlerMethod);
        return ResponseEntity.status(result.getHttpStatus()).body(result);
    }

    @ExceptionHandler(value = OdcUncheckedException.class)
    @ResponseBody
    public OdcErrorResult handleOdcUncheckedException(OdcUncheckedException e, HandlerMethod handlerMethod) {
        return handleUnknownException(e.getCheckedException(), handlerMethod);
    }

    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public OdcErrorResult handleBindException(BindException e, HandlerMethod handlerMethod) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        OdcErrorResult result = initErrorResult(ErrorCodes.BadRequest, e.getMessage());
        result.setHttpStatus(HttpStatus.BAD_REQUEST);
        result.setErrMsg(CollectionUtils.isEmpty(errors) ? null : errors.get(0).getDefaultMessage());
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected OdcErrorResult handleMissingServletRequestParameter(MissingServletRequestParameterException e,
            HandlerMethod handlerMethod) {
        String localizedMessage = e.getLocalizedMessage();
        OdcErrorResult result = initErrorResult(ErrorCodes.BadRequest, e.getMessage());
        result.setHttpStatus(HttpStatus.BAD_REQUEST);
        result.setErrMsg(localizedMessage);
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public OdcErrorResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
            HandlerMethod handlerMethod) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        OdcErrorResult result = initErrorResult(ErrorCodes.BadRequest, e.getMessage());
        result.setHttpStatus(HttpStatus.BAD_REQUEST);
        result.setErrMsg(CollectionUtils.isEmpty(errors) ? null : errors.get(0).getDefaultMessage());
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = RequestRejectedException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public OdcErrorResult handleRequestRejectedException(RequestRejectedException e, HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.BadRequest, e.getMessage());
        result.setHttpStatus(HttpStatus.BAD_REQUEST);
        result.setErrMsg(e.getLocalizedMessage());
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = HttpMediaTypeException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public OdcErrorResult handleHttpMediaTypeException(HttpMediaTypeException e, HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.BadRequest, e.getMessage());
        result.setHttpStatus(HttpStatus.BAD_REQUEST);
        result.setErrMsg(e.getLocalizedMessage());
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    public OdcErrorResult handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e,
            HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.BadRequest, e.getMessage());
        result.setHttpStatus(HttpStatus.METHOD_NOT_ALLOWED);
        result.setErrMsg(e.getLocalizedMessage());
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = NoHandlerFoundException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public OdcErrorResult handleNoHandlerFoundException(NoHandlerFoundException e, HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.BadRequest, e.getMessage());
        result.setErrMsg(e.getLocalizedMessage());
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public OdcErrorResult handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
            HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.BadRequest, e.getMessage());
        result.setHttpStatus(HttpStatus.BAD_REQUEST);
        result.setErrMsg(e.getMessage());
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = MockerException.class)
    @ResponseBody
    public OdcErrorResult handleMockerException(MockerException e, HandlerMethod handlerMethod) {
        OdcErrorResult result = OdcErrorResult.error(ErrorCodes.MockError, new Object[] {e.getMessage()});
        result.setHttpStatus(HttpStatus.BAD_REQUEST);
        attachTraceInfo(result);
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public OdcErrorResult handleIllegalArgumentException(IllegalArgumentException e, HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.IllegalArgument, e.getMessage());
        result.setHttpStatus(HttpStatus.BAD_REQUEST);
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    @ExceptionHandler(value = NullPointerException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public OdcErrorResult handleNullPointerException(NullPointerException e, HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.Unexpected, e.getMessage());
        result.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        logTraceInfo(result, e, handlerMethod);
        AlarmUtils.warn(AlarmEventNames.UNKNOWN_API_EXCEPTION, buildAlarmContent(handlerMethod, e));
        logExceptionStack(e);
        return result;
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public OdcErrorResult handleUnknownException(Exception e, HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.Unknown, e.getMessage());
        result.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        result.setErrMsg(e.getMessage());
        logTraceInfo(result, e, handlerMethod);
        logExceptionStack(e);
        AlarmUtils.warn(AlarmEventNames.UNKNOWN_API_EXCEPTION, buildAlarmContent(handlerMethod, e));
        return result;
    }

    /**
     * may Error, such as OutOfMemoryError, StackOverflowError
     */
    @ExceptionHandler(value = Throwable.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public OdcErrorResult handleThrowable(Throwable e, HandlerMethod handlerMethod) {
        OdcErrorResult result = initErrorResult(ErrorCodes.Unknown, e.getMessage());
        result.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        result.setErrMsg(e.getMessage());
        log.error("ODC_FATAL_ERROR: ", e);
        AlarmUtils.warn(AlarmEventNames.UNKNOWN_API_EXCEPTION, buildAlarmContent(handlerMethod, e));
        logTraceInfo(result, e, handlerMethod);
        return result;
    }

    /**
     * log trace info, includes request client info and root cause message/stack trace <br>
     * rootReason example: <br>
     * com.alipay.odc.exception.OdcException: connection not found by sid 44-1 at <br>
     * com.alipay.odc.odcsdk.sdk.connection.cache.ConnectionCacheManager.get(ConnectionCacheManager.java:68)
     */
    private void logTraceInfo(OdcErrorResult result, Throwable e, HandlerMethod handlerMethod) {
        String exceptionType = e.getClass().getSimpleName();
        String rootReason = ExceptionUtils.getRootCauseReason(e);
        String httpMethod = request.getMethod();
        String fullURL = WebRequestUtils.getRequestFullURL(request);
        String handler = handlerMethod.getShortLogMessage();
        String clientAddress = WebRequestUtils.getClientAddress(request);
        String perfLevel = LogUtils.perfLevel(result.getDurationMillis());
        TraceContextHolder.setOdcCode(result.getCode());

        log.info("ODC_FAILED_REQUEST, httpMethod={}, fullURL={}, handler={}, remoteHost={}, clientAddress={}, "
                + "exceptionType={}, message={}, rootReason={}, perfLevel={}, result={}",
                httpMethod, fullURL, handler, request.getRemoteHost(), clientAddress,
                exceptionType, e.getMessage(), rootReason, perfLevel, result);
    }

    private void logExceptionStack(Throwable e) {
        log.warn(e.getClass().getSimpleName() + ": ", e);
    }

    private OdcErrorResult initErrorResult(ErrorCode errorCode, String message) {
        return initErrorResult(errorCode, new Object[] {message});
    }

    public OdcErrorResult initErrorResult(ErrorCode errorCode, Object[] args) {
        try {
            OdcErrorResult result = OdcErrorResult.error(errorCode, args);
            return attachTraceInfo(result);
        } catch (Throwable throwable) {
            log.error("init error result failed, use empty result instead, reason={}", throwable.getMessage());
            return OdcErrorResult.empty();
        }
    }

    private OdcErrorResult attachTraceInfo(OdcErrorResult result) {
        result.setServer(server);
        result.setTraceId(TraceContextHolder.getTraceId());
        result.setDurationMillis(TraceContextHolder.getDuration());
        result.setTimestamp(OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(TraceContextHolder.getStartEpochMilli()), ZoneId.systemDefault()));
        return result;
    }

    private String buildAlarmContent(HandlerMethod handlerMethod, Throwable e) {
        return "method=" + handlerMethod.getMethod().getName() + "msg=" + e.getMessage();
    }

}
