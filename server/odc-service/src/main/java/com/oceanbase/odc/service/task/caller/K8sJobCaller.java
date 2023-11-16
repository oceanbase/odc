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

package com.oceanbase.odc.service.task.caller;

import com.oceanbase.odc.core.task.context.JobContext;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public class K8sJobCaller implements JobCaller {

    private final K8sClient client;

    public K8sJobCaller(K8sClient client) {
        this.client = client;
    }

    @Override
    public void start(JobContext context) throws JobException {
        // start a new k8s job
       // client.createNamespaceJob()
    }

    @Override
    public void stop(JobContext context) throws JobException {

    }
}
