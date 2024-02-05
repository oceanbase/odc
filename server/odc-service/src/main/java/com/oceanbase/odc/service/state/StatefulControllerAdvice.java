/*
 * Copyright (c) 2024 OceanBase.
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

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.oceanbase.odc.service.common.response.SuccessResponse;

@ControllerAdvice
public class StatefulControllerAdvice implements ResponseBodyAdvice<Object> {

    public static final String STATE_ID = "state_id";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!SuccessResponse.class.isAssignableFrom(returnType.getContainingClass())) {
            return false;
        }
        ResolvableType resolvableType = ResolvableType.forMethodParameter(returnType);
        if (resolvableType.hasGenerics()) {
            // 获取返回类型的泛型
            ResolvableType[] generics = resolvableType.getGenerics();
            // 假设我们只关心第一个泛型参数
            Class<?> genericType = generics[0].resolve();
            // 这里你可以检查泛型类型是否是你所期望的类型
            return StateIdResponse.class.equals(genericType);
        }
        return false;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        SuccessResponse response1 = (SuccessResponse) body;
        StateIdResponse data = (StateIdResponse) response1.getData();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest servletRequest = (HttpServletRequest) request;
            servletRequest.setAttribute(STATE_ID, data.stateId());
        }
        return body;
    }
}
