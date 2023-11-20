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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;

import com.oceanbase.odc.service.task.caller.K8sClient;
import com.oceanbase.odc.service.task.caller.PrimitivePodBasedK8sClient;
import com.oceanbase.odc.test.database.TestProperties;

/**
 * @author yaobin
 * @date 2023-11-17
 * @since 4.2.4
 */
public abstract class BaseJobTest {

    protected static K8sClient k8sClient;
    protected static String imageName;
    protected static List<String> cmd;

    @BeforeClass
    public static void init() throws IOException {
        k8sClient = new PrimitivePodBasedK8sClient(TestProperties.getProperty("odc.k8s.cluster.url"));
        imageName = "perl:5.34.0";
        cmd = Arrays.asList("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)");
    }

    public static K8sClient getK8sClient() {
        return k8sClient;
    }

    public static String getImageName() {
        return imageName;
    }

    public static List<String> getCmd() {
        return cmd;
    }
}
