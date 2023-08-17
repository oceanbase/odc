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

import static javax.servlet.RequestDispatcher.ERROR_EXCEPTION;
import static javax.servlet.RequestDispatcher.ERROR_STATUS_CODE;

import java.util.Objects;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.service.common.util.WebRequestUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * for record logging while page not found
 */
@Slf4j
@Controller
public class WebErrorController implements ErrorController {

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        // do something like logging
        String originalUrl = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        Exception originalError = (Exception) request.getAttribute(ERROR_EXCEPTION);
        Integer errorCode = (Integer) request.getAttribute(ERROR_STATUS_CODE);

        if (Objects.nonNull(originalUrl)) {
            log.warn(
                    "Requested URL not found or unexpected error occur: [url={}, client={}, traceId={}, originalErrorCode={}]",
                    originalUrl,
                    WebRequestUtils.getClientAddress(request), TraceContextHolder.getTraceId(), errorCode,
                    originalError);
        } else {
            log.info("Umm, Seems you call '/error' page straightly");
        }
        return new ModelAndView("404");
    }
}
