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
package com.oceanbase.odc.core.shared.rest;

import java.net.URI;
import java.util.LinkedList;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * RestTemplate的adaptor，主要增加了外部调用的日志功能
 */
@Slf4j
public final class OdcRestTemplate extends RestTemplate {

    private static final ThreadLocal<ODCRestContext> API_CALL_LOG_PREFIX = new ThreadLocal<>();

    private ODCRestContext templateContext = new ODCRestContext("restApi");

    public OdcRestTemplate(RestTemplate restTemplate) {
        super();
        setRequestFactory(restTemplate.getRequestFactory());
        setUriTemplateHandler(restTemplate.getUriTemplateHandler());
        setErrorHandler(restTemplate.getErrorHandler());
        setMessageConverters(restTemplate.getMessageConverters());
        LinkedList<ClientHttpRequestInterceptor> clientHttpRequestInterceptors = new LinkedList<>(
                restTemplate.getInterceptors());
        clientHttpRequestInterceptors.addFirst(new OdcRestLogInterceptor());
        setInterceptors(clientHttpRequestInterceptors);
        setClientHttpRequestInitializers(restTemplate.getClientHttpRequestInitializers());
    }

    public OdcRestTemplate(RestTemplate restTemplate, ODCRestContext templateContext) {
        this(restTemplate);
        this.templateContext = templateContext;
    }

    public OdcRestTemplate(RestTemplate restTemplate, String apiName, boolean logResponseEnabled) {
        this(restTemplate, new ODCRestContext(apiName, logResponseEnabled));
    }

    public OdcRestTemplate(RestTemplate restTemplate, String apiName) {
        this(restTemplate, new ODCRestContext(apiName));
    }

    static void clearContext() {
        API_CALL_LOG_PREFIX.remove();
    }

    static ODCRestContext get() {
        return API_CALL_LOG_PREFIX.get();
    }

    @Override
    protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
            @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {
        API_CALL_LOG_PREFIX.set(templateContext.createNew());
        try {
            T res = super.doExecute(url, method, requestCallback, responseExtractor);
            ODCRestContext context = API_CALL_LOG_PREFIX.get();
            if (context.getLogResponseEnabled()) {
                log.debug("success call rest {}, url={}, response={}, cost={}ms", context.getApiName(),
                        context.getRealUrl(), res,
                        context.getExecTime());
            } else {
                log.debug("success call rest {}, url={},  cost={}ms", context.getApiName(),
                        context.getRealUrl(),
                        context.getExecTime());
            }
            return res;
        } catch (Exception e) {
            ODCRestContext context = API_CALL_LOG_PREFIX.get();
            log.debug("failed call rest {}, URL:{}, cost={} ms ", context.getApiName(), context.getRealUrl(),
                    context.getExecTime(), e);
            throw new UnexpectedException("Internal service call failed, please contact support team.", e);
        } finally {
            clearContext();
        }
    }

    @AllArgsConstructor
    @Getter
    public static class ODCRestContext {
        @Setter
        private String apiName;
        private Boolean logResponseEnabled;

        private Long startTime;
        private Long endTime;
        @Setter
        private String realUrl;

        public ODCRestContext(String apiName) {
            this.apiName = apiName;
            this.logResponseEnabled = true;
        }

        public ODCRestContext(String apiName, boolean logResponseEnabled) {
            this.apiName = apiName;
            this.logResponseEnabled = logResponseEnabled;
        }

        public ODCRestContext createNew() {
            ODCRestContext context = new ODCRestContext(this.apiName);
            context.logResponseEnabled = this.logResponseEnabled;
            context.startTime = System.currentTimeMillis();
            return context;
        }

        private long getExecTime() {
            if (endTime == null) {
                endTime = System.currentTimeMillis();
            }
            return endTime - startTime;
        }
    }

}
