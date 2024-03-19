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
package com.oceanbase.odc.service.state;

import java.util.Optional;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.oceanbase.odc.service.dispatch.DispatchResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StatefulRouteInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            @Nullable ModelAndView modelAndView) throws Exception {
        DispatchResponse dispatchResponse =
                Optional.ofNullable(StateRouteFilter.getContext()).map(StateRouteContext::getDispatchResponse)
                        .orElse(null);
        if (dispatchResponse != null) {
            dispatchResponse.getResponseHeaders().forEach((headerName, headerValues) -> {
                headerValues.forEach(value -> response.setHeader(headerName, value));
            });
            response.setStatus(dispatchResponse.getHttpStatus().value());
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(dispatchResponse.getContent());
            outputStream.flush();
            outputStream.close();
        }
    }
}
