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
package com.oceanbase.odc.common.trace;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;

import com.oceanbase.odc.common.util.StringUtils;

/**
 * trace相关工具类 <br>
 * 基于MDC实现
 *
 * @author zhigang.xzg
 * @date 2019/12/26
 */
public final class TraceContextHolder {

    public static final String START_EPOCH_MILLI = "startEpochMilli";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String REQUEST_ID = "requestId";
    public static final String ACCOUNT_NAME = "accountName";
    public static final String USER_ID = "userId";
    public static final String CALLER_BID = "callerBid";
    public static final String ORGANIZATION_ID = "organizationId";
    public static final String RESPONSE_TIME = "rt";
    public static final String HTTP_CODE = "httpCode";
    public static final String ODC_CODE = "odcCode";

    private TraceContextHolder() {}

    /**
     * 请求入口处，将trace信息写入上下文
     */
    public static void trace() {
        put(START_EPOCH_MILLI, String.valueOf(Instant.now().toEpochMilli()));
        put(TRACE_ID, generateTraceId());
    }

    /**
     * 进入异步线程时，继承父线程trace上下文，同时将子线程的spanId写入上下文
     * 
     * @param traceContext 父线程trace上下文
     */
    public static void span(Map<String, String> traceContext) {
        if (traceContext != null) {
            MDC.setContextMap(traceContext);
        }
        put(SPAN_ID, generateSpanId());
    }

    /**
     * 获取trace上下文
     * 
     * @return MDC context
     */
    public static Map<String, String> getTraceContext() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * 清除 trace 上下文
     */
    public static void clear() {
        MDC.remove(START_EPOCH_MILLI);
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
        MDC.remove(REQUEST_ID);
        MDC.remove(ACCOUNT_NAME);
        MDC.remove(USER_ID);
        MDC.remove(CALLER_BID);
        MDC.remove(ORGANIZATION_ID);
    }

    public static long getStartEpochMilli() {
        final String startEpochMilli = get(START_EPOCH_MILLI);
        if (startEpochMilli == null) {
            return 0L;
        }
        return Long.parseLong(startEpochMilli);
    }

    /**
     * 获取当前请求耗时，单位milliseconds
     *
     * @return 获得当前请求耗时
     */
    public static long getDuration() {
        String rtString = get(RESPONSE_TIME);
        if (rtString == null) {
            final String startEpochMilli = get(START_EPOCH_MILLI);
            if (startEpochMilli == null) {
                put(RESPONSE_TIME, "0");
                return 0L;
            } else {
                long rt = System.currentTimeMillis() - Long.parseLong(startEpochMilli);
                put(RESPONSE_TIME, Long.toString(rt));
                return rt;
            }
        }
        return Long.parseLong(rtString);
    }

    /**
     * 获取用户请求全局跟踪id
     *
     * @return 16位小写字母加数字的组合
     */
    public static String getTraceId() {
        String traceId = get(TRACE_ID);
        return traceId == null ? "" : traceId;
    }

    public static void setTraceId(String traceId) {
        put(TRACE_ID, traceId);
    }

    public static String getRequestId() {
        String requestId = get(REQUEST_ID);
        return requestId == null ? "" : requestId;
    }

    public static void setRequestId(String request) {
        put(REQUEST_ID, request);
    }

    public static String getAccountName() {
        String accountName = get(ACCOUNT_NAME);
        return accountName == null ? "" : accountName;
    }

    public static void setAccountName(String accountName) {
        put(ACCOUNT_NAME, accountName);
    }

    public static String getOdcCode() {
        return get(ODC_CODE);
    }

    public static void setOdcCode(String odcCode) {
        put(ODC_CODE, odcCode);
    }

    public static Long getUserId() {
        String userIdStr = get(USER_ID);
        return StringUtils.isEmpty(userIdStr) ? null : Long.parseLong(userIdStr);
    }

    public static void setUserId(Long userId) {
        put(USER_ID, userId.toString());
    }

    public static String getCallerBid() {
        String callerBid = get(CALLER_BID);
        return StringUtils.isEmpty(callerBid) ? null : callerBid;
    }

    public static void setCallerBid(String callerBid) {
        put(CALLER_BID, callerBid);
    }

    public static Long getOrganizationId() {
        String userIdStr = get(ORGANIZATION_ID);
        return StringUtils.isEmpty(userIdStr) ? null : Long.parseLong(userIdStr);
    }

    public static void setOrganizationId(Long organizationId) {
        put(ORGANIZATION_ID, organizationId.toString());
    }

    public static String get(String key) {
        return MDC.get(key);
    }

    public static void put(String key, String value) {
        MDC.put(key, value);
    }

    public static void remove(String key) {
        MDC.remove(key);
    }

    private static String generateTraceId() {
        // 生成traceId（用户请求全局跟踪id）, 16位小写字母加数字的组合
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16).toLowerCase();
    }

    private static String generateSpanId() {
        // 生成spanId（用户请求在线程内的标识id）, 12位小写字母加数字的组合
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toLowerCase();
    }
}
