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
import java.util.regex.Pattern;

import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2024-01-26
 * @since 4.2.4
 */
public class ExecutorProcessBuilderFactory {

    private static final Pattern ODC_SERVER_EXECUTABLE_JAR = Pattern.compile("^.*odc-server-.*executable\\.jar$");

    public ProcessBuilder getProcessBuilder(ProcessConfig processConfig, long jobId, String executorName) {
        return getProcessBuilder(processConfig, jobId, executorName, JobConstants.ODC_AGENT_CLASS_NAME);
    }

    public ProcessBuilder getProcessBuilder(ProcessConfig processConfig, long jobId, String executorName,
            String mainClassName) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        ProcessBuilder pb = new ProcessBuilder();
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-D" + JobUtils.generateExecutorSelectorOnProcess(executorName));
        commands.addAll(jvmOptions(processConfig, jobId));
        if (ODC_SERVER_EXECUTABLE_JAR.matcher(runtimeMxBean.getClassPath()).matches()) {
            // start odc executor by java -cp
            commands.add("-cp");
            // set jar package file name in commands
            commands.add(runtimeMxBean.getClassPath());
            commands.add("-Dloader.main=" + mainClassName);
            commands.add("org.springframework.boot.loader.PropertiesLauncher");
        } else {
            // start odc executor by java -classpath
            commands.add("-cp");
            commands.add(runtimeMxBean.getClassPath());
            commands.add(mainClassName);
            // commands.add("org.springframework.boot.loader.PropertiesLauncher");
        }
        pb.command(commands);
        pb.directory(new File("."));
        pb.environment().putAll(processConfig.getEnvironments());
        return pb;
    }

    private List<String> jvmOptions(ProcessConfig processConfig, long jobId) {
        List<String> options = new ArrayList<>();
        options.add("-XX:+UseG1GC");
        options.add("-XX:+PrintAdaptiveSizePolicy");
        options.add("-XX:+PrintGCDetails");
        options.add("-XX:+PrintGCTimeStamps");
        options.add("-XX:+PrintGCDateStamps");
        options.add(String.format("-Xloggc:%s/task/%d/gc.log",
                processConfig.getEnvironments().get(JobEnvKeyConstants.ODC_LOG_DIRECTORY), jobId));
        options.add("-XX:+UseGCLogFileRotation");
        options.add("-XX:GCLogFileSize=50M");
        options.add("-XX:NumberOfGCLogFiles=5");
        options.add("-XX:+ExitOnOutOfMemoryError");
        options.add(String.format("-Xmx%dm", processConfig.getJvmXmxMB()));
        options.add(String.format("-Xms%dm", processConfig.getJvmXmsMB()));
        return options;
    }

}
