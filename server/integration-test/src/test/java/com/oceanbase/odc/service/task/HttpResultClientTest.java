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

package com.oceanbase.odc.service.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.executor.DefaultTaskResult;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
@Ignore("manual run test case")
@Slf4j
public class HttpResultClientTest {

    @Test
    public void test_HttpResultClient() throws InterruptedException {

        OkHttpClient client = new OkHttpClient.Builder()
                .protocols(Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_2))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(1, TimeUnit.MINUTES)
                .build();

        String url = "http://localhost:8989/api/v2/task/result";

        DefaultTaskResult result = new DefaultTaskResult();
        result.setProgress(60.0);
        result.setTaskStatus(TaskStatus.RUNNING);
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody requestBody = RequestBody.create(JsonUtils.toJson(result), mediaType);

        Request request = new Request.Builder().post(requestBody)
                .addHeader("Content-Type", "application/json")
                .url(url)
                .build();

        CountDownLatch latch = new CountDownLatch(1);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.warn(JsonUtils.toJson(e.getMessage()));
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log.info(JsonUtils.toJson(response));
                latch.countDown();

            }
        });

        latch.await(60, TimeUnit.SECONDS);

    }


}
