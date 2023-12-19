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

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.LogUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.HttpException;
import com.oceanbase.odc.server.web.trace.TraceSuccessResponseAdvice;
import com.oceanbase.odc.service.common.exception.OdcUncheckedException;
import com.oceanbase.odc.service.common.response.Error;
import com.oceanbase.odc.service.common.response.ErrorResponse;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

import lombok.extern.slf4j.Slf4j;

/**
 * global exception handler for v2 api, <br>
 * refer Spring mvc default behavior from {@link DefaultHandlerExceptionResolver} and
 * {@link ResponseEntityExceptionHandler} <br>
 * will attach trance info for error response, for success response part, refer
 * {@link TraceSuccessResponseAdvice}
 *
 * @author yizhou.xw
 */
@ControllerAdvice(basePackages = {"com.oceanbase.odc.server.web.controller.v2"})
@Slf4j
public class RestExceptionHandler {

    private static final String server = SystemUtils.getHostName();

    @Autowired
    private HttpServletRequest request;

    /**
     * Handle ConstraintViolationException. Thrown when @Validated fails.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleConstraintViolation(ConstraintViolationException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    /**
     * Inspects the cause for different DB causes.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex,
            HandlerMethod handlerMethod) {
        if (ex.getCause() instanceof ConstraintViolationException) {
            return handleConstraintViolation((ConstraintViolationException) ex.getCause(), handlerMethod);
        }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, Error.of(ErrorCodes.DataAccessError),
                ex, handlerMethod);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadArgument);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(MethodArgumentConversionNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleMethodArgumentConversionNotSupportedException(
            MethodArgumentConversionNotSupportedException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadArgument);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(ConversionNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleConversionNotSupportedException(ConversionNotSupportedException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadArgument);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    /**
     * Triggered when a 'required' request parameter is missing.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    /**
     * Triggered when a 'required' path variable is missing.
     */
    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleMissingPathVariableException(MissingPathVariableException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    /**
     * Triggered when JSON is invalid as well.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    protected ErrorResponse handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.addDetail(ex);
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, error, ex, handlerMethod);
    }

    /**
     * Triggered when an object fails @Valid validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(value = RequestRejectedException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleRequestRejectedException(RequestRejectedException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.setMessage(ex.getLocalizedMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(value = HttpMediaTypeException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMediaTypeException(HttpMediaTypeException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.setMessage(ex.getLocalizedMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.setMessage(ex.getLocalizedMessage());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, error, ex, handlerMethod);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.addDetail(ex);
        error.setMessage(ex.getLocalizedMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
            HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.InternalServerError);
        error.addDetail(ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, error, ex, handlerMethod);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleNoHandlerFoundException(NoHandlerFoundException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(PropertyAccessException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePropertyAccessException(PropertyAccessException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(BindException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBindException(BindException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.BadRequest);
        error.addBindingResult(ex.getBindingResult());
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<ErrorResponse> handleHttpException(HttpException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ex);
        error.addDetail(ex);
        ErrorResponse response = buildResponse(ex.httpStatus(), error, ex, handlerMethod);
        return ResponseEntity.status(ex.httpStatus()).body(response);
    }

    @ExceptionHandler(OdcUncheckedException.class)
    public ErrorResponse handleOdcUncheckedException(OdcUncheckedException ex, HandlerMethod handlerMethod) {
        return handleUnknownException(ex.getCheckedException(), handlerMethod);
    }

    @ExceptionHandler(MockerException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMockerException(MockerException ex, HandlerMethod handlerMethod) {
        Error error = Error.ofBadRequest(ex);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex, HandlerMethod handlerMethod) {
        Error error = Error.ofBadRequest(ex);
        error.addDetail(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, error, ex, handlerMethod);
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNullPointerException(NullPointerException ex, HandlerMethod handlerMethod) {
        boolean isValidateException = StringUtils.isNotEmpty(ex.getMessage());
        if (!isValidateException) {
            log.warn("Unhandled NullPointerException: ", ex);
            AlarmUtils.warn(AlarmEventNames.UNKNOWN_API_EXCEPTION, buildAlarmContent(handlerMethod, ex));
        }
        Error error = isValidateException ? Error.ofBadRequest(ex) : Error.ofUnexpected(ex);
        error.addDetail(ex);
        HttpStatus httpStatus = isValidateException ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        return buildResponse(httpStatus, error, ex, handlerMethod);
    }

    @ExceptionHandler({IndexOutOfBoundsException.class, IllegalStateException.class})
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleOtherIllegalException(RuntimeException ex, HandlerMethod handlerMethod) {
        log.warn("Unhandled unchecked exception: ", ex);
        AlarmUtils.warn(AlarmEventNames.UNKNOWN_API_EXCEPTION, buildAlarmContent(handlerMethod, ex));
        Error error = Error.ofUnexpected(ex);
        error.addDetail(ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, error, ex, handlerMethod);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnknownException(Exception ex, HandlerMethod handlerMethod) {
        log.warn("Unhandled checked exception: ", ex);
        AlarmUtils.warn(AlarmEventNames.UNKNOWN_API_EXCEPTION, buildAlarmContent(handlerMethod, ex));
        Error error = Error.ofUnexpected(ex);
        error.addDetail(ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, error, ex, handlerMethod);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleThrowable(Throwable ex, HandlerMethod handlerMethod) {
        log.warn("Unhandled checked exception: ", ex);
        AlarmUtils.warn(AlarmEventNames.UNKNOWN_API_EXCEPTION, buildAlarmContent(handlerMethod, ex));
        Error error = Error.ofUnexpected(ex);
        error.addDetail(ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, error, ex, handlerMethod);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex, HandlerMethod handlerMethod) {
        Error error = Error.of(ErrorCodes.AccessDenied);
        error.addDetail(ex);
        return buildResponse(HttpStatus.FORBIDDEN, error, ex, handlerMethod);
    }

    private ErrorResponse buildResponse(HttpStatus status, Error error, Throwable e, HandlerMethod handlerMethod) {
        ErrorResponse response = new ErrorResponse(status, error);
        response.setServer(server);
        response.setTraceId(TraceContextHolder.getTraceId());
        response.setRequestId(TraceContextHolder.getRequestId());
        response.setDurationMillis(TraceContextHolder.getDuration());
        response.setTimestamp(OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(TraceContextHolder.getStartEpochMilli()), ZoneId.systemDefault()));
        logTraceInfo(response, e, handlerMethod);
        return response;
    }

    private void logTraceInfo(ErrorResponse response, Throwable e, HandlerMethod handlerMethod) {
        String exceptionType = e.getClass().getSimpleName();
        String rootReason = ExceptionUtils.getRootCauseReason(e);
        String httpMethod = request.getMethod();
        String fullURL = WebRequestUtils.getRequestFullURL(request);
        String clientAddress = WebRequestUtils.getClientAddress(request);
        String handler = handlerMethod.getShortLogMessage();
        String perfLevel = LogUtils.perfLevel(response.getDurationMillis());
        TraceContextHolder.setOdcCode(response.getCode());
        log.info("ODC_FAILED_REQUEST, httpMethod={}, fullURL={}, remoteHost={}, clientAddress={},"
                + "  handler={}, exceptionType={}, message={}, rootReason={}, perfLevel={}, response={}",
                httpMethod, fullURL, request.getRemoteHost(), clientAddress,
                handler, exceptionType, e.getMessage(), rootReason, perfLevel, response);
    }

    private String buildAlarmContent(HandlerMethod handlerMethod, Throwable e) {
        return "method=" + handlerMethod.getMethod().getName() + "msg=" + e.getMessage();
    }
}
