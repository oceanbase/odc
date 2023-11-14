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

package com.oceanbase.odc.service.k8s;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Watch.Response;
import io.kubernetes.client.util.Yaml;

/**
 * @author yaobin
 * @date 2023-11-13
 * @since 4.2.4
 */
public class K8sClientTest {

    @BeforeClass
    public static void initClient() throws IOException {
        ApiClient client = Config.defaultClient();
        client.setBasePath("http://11.124.9.79:8981");
        Configuration.setDefaultApiClient(client);
    }

    @Test
    public void test_get_pods() throws ApiException {
        CoreV1Api api = new CoreV1Api();
        V1PodList list = api.listPodForAllNamespaces(
                null, null, null, null, null,
                null, null, null, null, null);
        for (V1Pod item : list.getItems()) {
            System.out.println(item.getMetadata().getName());
        }
    }

    @Test
    public void test_create_job() throws ApiException {
        BatchV1Api api = new BatchV1Api();

        V1Job job = null;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("k8s/pi-job.yaml");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            job = (V1Job) Yaml.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        V1Job createdJob =
                api.createNamespacedJob("default", job, null, null, null, null);
        System.out.println("job created: " + createdJob.getMetadata().getName());
    }


    @Test
    public void test_Watch() throws ApiException, IOException, InterruptedException {
        CoreV1Api api = new CoreV1Api();

        Watch<V1Pod> watch = Watch.createWatch(
                Configuration.getDefaultApiClient(),
                api.listPodForAllNamespacesCall(null, null, null, null,
                        null, null, null, null, 60, null, null),
                new TypeToken<Response<V1Pod>>() {}.getType());

        // Start the watch and process the events
        for (Watch.Response<V1Pod> response : watch) {
            if (response.type == null) {
                return;
            }
            if (response.type.equals("ADDED")) {
                V1Pod pod = response.object;
                System.out.println("New Pod added: " + pod.getMetadata().getName());
            } else if (response.type.equals("MODIFIED")) {
                V1Pod pod = response.object;
                System.out.println("Pod modified: " + pod.getMetadata().getName());
            } else if (response.type.equals("DELETED")) {
                V1Pod pod = response.object;
                System.out.println("Pod deleted: " + pod.getMetadata().getName());
            }
        }

        // Close the watch
        watch.close();

        // Wait for a certain period of time to keep the program running
        TimeUnit.SECONDS.sleep(10);
    }

}
