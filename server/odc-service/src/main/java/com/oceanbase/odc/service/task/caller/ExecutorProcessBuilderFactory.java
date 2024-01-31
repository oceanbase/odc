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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.service.task.constants.JobConstants;

/**
 * @author yaobin
 * @date 2024-01-26
 * @since 4.2.4
 */
public class ExecutorProcessBuilderFactory {

    public ProcessBuilder getProcessBuilder(Map<String, String> environments, String executorName) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        ProcessBuilder pb = new ProcessBuilder();
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-D" + JobConstants.ODC_EXECUTOR_PROCESS_PROPERTIES_KEY + "=" + executorName);
        commands.addAll(jvmOptions());
        commands.add("-classpath");
        commands.add(runtimeMxBean.getClassPath());
        commands.add(JobConstants.ODC_SERVER_CLASS_NAME);
        pb.directory(new File("."));
        pb.command(commands);
        pb.environment().putAll(environments);
        return pb;
    }

    private List<String> jvmOptions() {
        List<String> options = new ArrayList<>();
        options.add("-XX:+UseG1GC");
        options.add("-XX:+PrintAdaptiveSizePolicy");
        options.add("-XX:+PrintGCDetails");
        options.add("-XX:+PrintGCTimeStamps");
        options.add("-XX:+PrintGCDateStamps");
        options.add("-XX:+UseGCLogFileRotation");
        options.add("-XX:GCLogFileSize=50M");
        options.add("-XX:NumberOfGCLogFiles=5");
        options.add("-XX:+ExitOnOutOfMemoryError");
        options.add("-Xmx2048m");
        options.add("-Xms2048m");
        return options;
    }

}
