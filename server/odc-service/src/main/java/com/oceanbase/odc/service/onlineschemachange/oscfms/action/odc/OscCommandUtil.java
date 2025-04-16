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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.odc;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.oceanbase.odc.common.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2025/3/24 14:20
 */
@Slf4j
public class OscCommandUtil {
    private static final Integer HTTP_SUCCESS = 200;
    private static final Integer HTTP_CREATED = 201;

    public static boolean isOSCMigrateSupervisorAlive(String oscSupervisorEndpoint) {
        SupervisorResponse response = heartbeat(oscSupervisorEndpoint);
        return null != response && response.isSuccess();
    }

    public static SupervisorResponse heartbeat(String oscSupervisorEndpoint) {
        try {
            String response = get(oscSupervisorEndpoint + "/heartbeat", 3000);
            return JsonUtils.fromJson(response, SupervisorResponse.class);
        } catch (Throwable e) {
            log.warn("heartbeat osc migrate supervisor failed, cause={}", e.getMessage());
            return failedResponse(e.getMessage(), Collections.emptyMap());
        }
    }


    public static SupervisorResponse startTask(String oscSupervisorEndpoint, Map<String, String> parameters) {
        try {
            String response = post(oscSupervisorEndpoint + "/start", JsonUtils.toJson(parameters), 10000);
            return JsonUtils.fromJson(response, SupervisorResponse.class);
        } catch (Throwable e) {
            log.warn("start osc migrate supervisor failed, cause={}", e.getMessage());
            return failedResponse(e.getMessage(), Collections.emptyMap());
        }
    }

    public static SupervisorResponse monitorTask(String oscSupervisorEndpoint) {
        try {
            String response = get(oscSupervisorEndpoint + "/monitor", 5000);
            return JsonUtils.fromJson(response, SupervisorResponse.class);
        } catch (Throwable e) {
            log.warn("monitor osc migrate supervisor failed, cause={}", e.getMessage());
            return failedResponse(e.getMessage(), Collections.emptyMap());
        }
    }

    public static SupervisorResponse clearTask(String oscSupervisorEndpoint) {
        try {
            String response = get(oscSupervisorEndpoint + "/clear", 5000);
            return JsonUtils.fromJson(response, SupervisorResponse.class);
        } catch (Throwable e) {
            log.warn("monitor osc migrate supervisor failed, cause={}", e.getMessage());
            return failedResponse(e.getMessage(), Collections.emptyMap());
        }
    }

    public static SupervisorResponse updateTask(String oscSupervisorEndpoint, int rps) {
        try {
            Map<String, String> updatedConfig =
                    Collections.singletonMap("throttleRps", String.valueOf(Math.max(1, rps)));
            String response = post(oscSupervisorEndpoint + "/update", JsonUtils.toJson(updatedConfig), 5000);
            return JsonUtils.fromJson(response, SupervisorResponse.class);
        } catch (Throwable e) {
            log.warn("update osc migrate supervisor config with rps= {}, failed, cause={}", rps, e.getMessage());
            return failedResponse(e.getMessage(), Collections.emptyMap());
        }
    }


    private static SupervisorResponse failedResponse(String e, Map<String, String> responseData) {
        return new SupervisorResponse(false, e, responseData);
    }

    private static String get(String url, int timeoutMs) throws Exception {
        return get(url, Collections.emptyMap(), Collections.emptyMap(), timeoutMs);
    }

    private static String get(String url, Map<String, String> params, Map<String, String> headers, int timeoutInMs)
            throws Exception {
        long timeBegin = System.currentTimeMillis();

        if (null != params && !params.isEmpty()) {
            List<NameValuePair> parameters = new ArrayList<>();
            for (String key : params.keySet()) {
                parameters.add(new BasicNameValuePair(key, params.get(key)));
            }

            String paramStr = URLEncodedUtils.format(parameters, StandardCharsets.UTF_8);
            url = url + "?" + paramStr;
        }
        if (log.isDebugEnabled()) {
            log.debug("sending http Get Request. url: " + url);
        }

        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(RequestConfig.custom().setSocketTimeout(timeoutInMs).setConnectTimeout(timeoutInMs)
                .setConnectionRequestTimeout(timeoutInMs).build());

        if (headers != null && !headers.isEmpty()) {
            for (String key : headers.keySet()) {
                httpGet.setHeader(key, headers.get(key));
            }
        }
        RequestConfig config = RequestConfig.custom().setRedirectsEnabled(false).build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build();
        CloseableHttpResponse response = httpClient.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();

        String result = null;
        String errorMessage = "";
        try {
            HttpEntity entity = response.getEntity();
            result = IOUtils.toString(entity.getContent());
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error(String.format("GET [%s] failed with status [%d]", url, status), e);
        }

        response.close();
        httpClient.close();

        if (log.isInfoEnabled()) {
            long timeFinish = System.currentTimeMillis();
            log.info("[GET] [{}] [{}] [{}] [{}]", url,
                    status, timeFinish - timeBegin, errorMessage);
        }

        if (HTTP_SUCCESS == status) {
            return result;
        }
        throw new IllegalStateException("GET " + url + " failed with status =" + status + ", result = " + result);
    }

    private static String post(String url, String json, int timeOutMs) throws Exception {
        StringEntity entity = new StringEntity(json, StandardCharsets.UTF_8);
        entity.setContentType("application/json");
        return post(url, entity, Collections.emptyMap(), timeOutMs);
    }

    private static String post(String url, HttpEntity reqEntity, Map<String, String> headers, int timeOutMs)
            throws Exception {
        URL urlInstance = new URL(url);

        long timeBegin = System.currentTimeMillis();

        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(RequestConfig.custom().setSocketTimeout(timeOutMs).setConnectTimeout(timeOutMs)
                .setConnectionRequestTimeout(timeOutMs).build());
        httpPost.setEntity(reqEntity);

        if (headers != null && !headers.isEmpty()) {
            for (String key : headers.keySet()) {
                httpPost.setHeader(key, headers.get(key));
            }
        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = httpClient.execute(httpPost);
        int status = response.getStatusLine().getStatusCode();

        String result = null;
        String errorMessage = "";
        try {
            HttpEntity entity = response.getEntity();
            result = IOUtils.toString(entity.getContent());
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error(String.format("POST [%s] failed with status [%d]", url, status), e);
        }

        response.close();
        httpClient.close();

        if (log.isInfoEnabled()) {
            long timeFinish = System.currentTimeMillis();
            log.info("[POST] [{}] [{}] [{}] [{}] [{}]", urlInstance.getHost(), urlInstance.getPath(),
                    status, timeFinish - timeBegin, errorMessage);
        }

        if (HTTP_SUCCESS == status || HTTP_CREATED == status) {
            return result;
        }
        throw new IllegalStateException("POST " + url + " failed with status =" + status + ", result = " + result);
    }
}
