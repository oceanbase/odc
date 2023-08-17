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
package com.oceanbase.odc.service.common.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.springframework.beans.TypeMismatchException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.HttpException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;

import lombok.Data;
import lombok.Getter;

/**
 * refer from
 * https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#7102-error-condition-responses
 * 
 * @author yizhou.xw
 * @version : Error.java, v 0.1 2021-02-19 14:11
 */
@Data
public class Error {
    private String code;
    private String message;
    private List<Detail> details = new ArrayList<>();

    public static Error of(HttpException ex) {
        String code = ex.getErrorCode().code();

        Error error = new Error();
        error.setCode(code);
        error.setMessage(ex.getLocalizedMessage());

        Detail detail = new Detail();
        detail.setCode(code);
        detail.setMessage(ex.getMessage());

        error.addDetail(detail);
        return error;
    }

    public static Error ofUnexpected(Throwable throwable) {
        String message = String.format("Unhandled exception, type=%s, message=%s",
                throwable.getClass().getSimpleName(), throwable.getMessage());
        UnexpectedException wrapper = new UnexpectedException(message, throwable);
        return of(wrapper);
    }

    public static Error ofBadRequest(Exception ex) {
        String message = String.format("BadRequest exception, type=%s, message=%s",
                ex.getClass().getSimpleName(), ex.getMessage());
        BadRequestException wrapper = new BadRequestException(message, ex);
        return of(wrapper);
    }

    public static Error ofBadRequest(Throwable throwable) {
        String message = String.format("BadRequest exception, type=%s, message=%s",
                throwable.getClass().getSimpleName(), throwable.getMessage());
        BadRequestException wrapper = new BadRequestException(message, throwable);
        return of(wrapper);
    }

    public static Error of(ErrorCode errorCode) {
        return of(errorCode, null);
    }

    public static Error of(MethodArgumentNotValidException ex) {
        Error error = new Error();
        error.addBindingResult(ex.getBindingResult());
        error.setCode(ErrorCodes.BadRequest.code());
        error.setMessage(
                ErrorCodes.BadRequest.getLocalizedMessage(new Object[] {error.getDetails().get(0).getMessage()}));
        return error;
    }

    public static Error of(ConstraintViolationException ex) {
        Error error = Error.of(ErrorCodes.IllegalArgument, new Object[] {"N/A", "Unknown"});
        Set<ConstraintViolation<?>> constraintViolationSet = ex.getConstraintViolations();
        if (constraintViolationSet.size() != 0) {
            ConstraintViolation<?> violation = constraintViolationSet.iterator().next();
            error = Error.of(ErrorCodes.IllegalArgument,
                    new Object[] {violation.getPropertyPath().toString(), violation.getMessage()});
        }
        constraintViolationSet.forEach(error::addDetail);
        return error;
    }

    public static Error of(ErrorCode errorCode, Object[] args) {
        Error error = new Error();
        error.setCode(errorCode.code());
        error.setMessage(errorCode.getLocalizedMessage(args));
        return error;
    }

    public void addDetail(Detail detail) {
        details.add(detail);
    }

    public void addDetail(ConstraintViolation<?> violation) {
        Detail detail = new Detail();
        detail.setCode(ErrorCodes.BadRequest.code());
        detail.setTarget(violation.getPropertyPath().toString());
        detail.setMessage(violation.getMessage());
        details.add(detail);
    }

    public void addDetail(Throwable ex) {
        Detail detail = new Detail();
        detail.fillException(ex);
        details.add(detail);
    }

    public void addFieldError(FieldError fieldError) {
        Detail detail = new Detail();
        detail.setCode(fieldError.getCode());
        detail.setTarget(fieldError.getField());
        detail.setMessage(fieldError.getDefaultMessage());
        details.add(detail);
    }

    public void addObjectError(ObjectError objectError) {
        Detail detail = new Detail();
        detail.setCode(objectError.getCode());
        detail.setTarget(objectError.getObjectName());
        detail.setMessage(objectError.getDefaultMessage());
        details.add(detail);
    }

    public void addDetail(MethodArgumentTypeMismatchException ex) {
        Detail detail = new Detail() {
            @Getter
            private String argumentName = ex.getName();
            @Getter
            private String requiredType = ex.getRequiredType().getName();
            {
                setTarget("Argument '" + argumentName + "'");
            }
        };
        detail.fillException(ex);
        detail.setCode("MethodArgumentTypeMismatch");
        details.add(detail);
    }

    public void addDetail(MethodArgumentConversionNotSupportedException ex) {

        Detail detail = new Detail() {
            @Getter
            private String argumentName = ex.getName();
            @Getter
            private String requiredType = ex.getRequiredType().getName();
            {
                setTarget("Argument '" + argumentName + "'");
            }
        };
        detail.fillException(ex);
        detail.setCode("MethodArgumentConversionNotSupported");
        details.add(detail);
    }

    public void addDetail(TypeMismatchException ex) {
        Detail detail = new Detail() {
            @Getter
            private String argumentName = ex.getPropertyName();
            @Getter
            private String requiredType = ex.getRequiredType().getName();
            {
                setTarget("Argument '" + argumentName + "'");
            }
        };
        detail.fillException(ex);
        detail.setCode("TypeMismatch");
        details.add(detail);
    }

    public void addDetail(MissingPathVariableException ex) {
        Detail detail = new Detail() {
            @Getter
            private String variableName = ex.getVariableName();
            {
                setTarget("PathVariable '" + variableName + "'");
            }
        };
        detail.fillException(ex);
        details.add(detail);
    }

    public void addDetail(MissingServletRequestParameterException ex) {
        Detail detail = new Detail() {
            @Getter
            private String parameterName = ex.getParameterName();
            @Getter
            private String parameterType = ex.getParameterType();
            {
                setTarget("RequestParameter '" + parameterName + "'");
            }
        };
        detail.fillException(ex);
        details.add(detail);
    }

    public void addBindingResult(BindingResult bindingResult) {
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            this.addFieldError(fieldError);
        }
        for (ObjectError objectError : bindingResult.getGlobalErrors()) {
            this.addObjectError(objectError);
        }
    }

    @Data
    public static class Detail {
        private String code;
        private String target;
        private String message;

        private void fillException(Throwable ex) {
            this.code = ex.getClass().getSimpleName();
            this.message = ex.getMessage();
        }
    }

}
