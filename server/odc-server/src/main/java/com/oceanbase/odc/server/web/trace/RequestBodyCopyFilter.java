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
package com.oceanbase.odc.server.web.trace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oceanbase.odc.service.dispatch.HttpRequestProvider;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter for {@link HttpServletRequest#getInputStream()}
 *
 * @author yh263208
 * @date 2022-03-25 10:47
 * @since ODC-release_3.3.0
 */
@Slf4j
public class RequestBodyCopyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter((HttpServletRequest) Proxy.newProxyInstance(RequestBodyCopyFilter.class.getClassLoader(),
                new Class[] {HttpServletRequest.class}, new ServletRequestInvocationHandler(request)), response);
    }

    private static void setAttribute(@NonNull String key, @NonNull Object value) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(key, value, RequestAttributes.SCOPE_REQUEST);
        }
    }

    static class ServletRequestInvocationHandler implements InvocationHandler {

        private final HttpServletRequest target;
        private final static String TARGET_METHOD_NAME = "getInputStream";

        private final BodyHolder bodyHolder = new BodyHolder();

        public ServletRequestInvocationHandler(@NonNull HttpServletRequest request) {
            this.target = request;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!TARGET_METHOD_NAME.equalsIgnoreCase(method.getName())) {
                return method.invoke(target, args);
            }
            if (bodyHolder.cached) {
                return new ByteArrayServletInputStream(bodyHolder.outputStream.toByteArray());
            }

            return new CopyOnReadInputStreamProxy(target.getInputStream(), bodyHolder);
        }
    }


    static class ByteArrayServletInputStream extends ServletInputStream {

        private ByteArrayInputStream byteArrayInputStream;

        public ByteArrayServletInputStream(byte[] data) {
            this.byteArrayInputStream = new ByteArrayInputStream(data);
        }

        @Override
        public int read() throws IOException {
            return byteArrayInputStream.read();
        }

        @Override
        public boolean isFinished() {
            return byteArrayInputStream.available() <= 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    static class BodyHolder {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean cached = false;
    }

    static class CopyOnReadInputStreamProxy extends ServletInputStream {

        private final ServletInputStream target;
        private final BodyHolder bodyHolder;
        private int currentSize = 0;
        private final static int MAX_CACHE_SIZE_BYTE = 2 * 1024 * 1024; // 2 MB

        public CopyOnReadInputStreamProxy(@NonNull ServletInputStream target, BodyHolder bodyHolder) {
            this.target = target;
            this.bodyHolder = bodyHolder;
        }

        @Override
        public boolean isFinished() {
            return target.isFinished();
        }

        @Override
        public boolean isReady() {
            return bodyHolder.cached || target.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            target.setReadListener(listener);
        }

        @Override
        public int read() throws IOException {
            int result = target.read();
            if (result != -1 && currentSize++ < MAX_CACHE_SIZE_BYTE) {
                bodyHolder.outputStream.write(result);
            } else {
                try {
                    bodyHolder.outputStream.close();
                    bodyHolder.cached = true;
                    setRequestBody(bodyHolder.outputStream);
                } catch (Exception e) {
                    log.warn("Failed to set request body", e);
                }
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            target.close();
        }

        private void setRequestBody(ByteArrayOutputStream requestBody) {
            setAttribute(HttpRequestProvider.REQUEST_BODY_KEY, requestBody);
        }
    }

}
