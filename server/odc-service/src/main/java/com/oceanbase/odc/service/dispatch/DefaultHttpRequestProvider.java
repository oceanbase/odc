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

package com.oceanbase.odc.service.dispatch;

import java.io.ByteArrayOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.oceanbase.odc.core.shared.Verify;

import lombok.NonNull;

public class DefaultHttpRequestProvider implements HttpRequestProvider {

    @Override
    public ByteArrayOutputStream getRequestBody() {
        return getValue(HttpRequestProvider.REQUEST_BODY_KEY, ByteArrayOutputStream.class);
    }

    @Override
    public HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    @SuppressWarnings("all")
    private static <T> T getValue(@NonNull String key, Class<T> clazz) {
        ServletRequestAttributes attributes = getRequestAttributes();
        Object value = attributes.getAttribute(key, RequestAttributes.SCOPE_REQUEST);
        if (value == null) {
            return null;
        } else if (!clazz.equals(value.getClass())) {
            return null;
        }
        return (T) value;
    }

    private static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Verify.notNull(requestAttributes, "RequestAttributes");
        return (ServletRequestAttributes) requestAttributes;
    }

}


