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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobConfiguration;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.PluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.TxtWriterPluginParameter;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

public class DataXTransferJob extends AbstractJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");
    private static final Pattern LOG_STATISTICS_PATTERN =
            Pattern.compile("^.+Total (\\d+) records, (\\d+) bytes.+Error (\\d+) records, (\\d+) bytes.+$");
    private static final Pattern LOG_DIRTY_RECORD_PATTERN =
            Pattern.compile("^.+exception.+record.+type$");
    private static final Pattern DATA_FILE_PATTERN =
            Pattern.compile("(^\"?(.+)\"?.(sql|csv|dat|txt))__(.+)$", Pattern.CASE_INSENSITIVE);

    private final JobConfiguration jobConfig;
    private final File workingDir;
    private final File logDir;

    private long failed;
    private Process process;

    public DataXTransferJob(ObjectResult object, JobConfiguration jobConfig, File workingDir, File logDir) {
        super(object);
        this.jobConfig = jobConfig;
        this.workingDir = workingDir;
        this.logDir = logDir;
    }

    @Override
    public void run() throws Exception {
        File dataxHome = Paths.get(workingDir.getPath(), "datax").toFile();
        if (!dataxHome.exists()) {
            throw new FileNotFoundException(dataxHome.getPath());
        }
        ExecutorService executor =
                Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("datax-monitor-%d").build());
        try {
            String[] cmdArray =
                    buildDataXExecutorCmd(dataxHome.getPath(), generateConfigurationFile().getPath());
            process = new ProcessBuilder().command(cmdArray).start();
            executor.submit(() -> {
                try {
                    analysisStatisticsLog(process.getInputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            // exit code: 0=success, 1=error
            int exitValue = process.waitFor();
            if (exitValue == 0) {
                renameExportFile();
                setStatus(Status.SUCCESS);
            } else {
                setStatus(Status.FAILURE);
                throw new RuntimeException(String.format("DataX task failed. Number of failed records: %d .", failed));
            }
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
            executor.shutdown();
        }
    }

    @Override
    public void cancel() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        super.cancel();
    }

    private File generateConfigurationFile() throws IOException {
        File file = Paths.get(workingDir.getPath(), "job.conf").toFile();
        if (file.exists()) {
            FileUtils.deleteQuietly(file);
        }
        FileUtils.write(file, JsonUtils.toJson(ImmutableMap.of("job", jobConfig)), StandardCharsets.UTF_8);
        return file;
    }

    private String[] buildDataXExecutorCmd(String dataxHomePath, String tmpFilePath) {
        String[] customJvmParams = jobConfig.getJvmParams();
        Set<String> jvmParams = Sets
                .newHashSet(ArrayUtils.isEmpty(customJvmParams) ? Constants.DEFAULT_DATAX_JVM_PARAMS : customJvmParams);
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-server");
        command.addAll(jvmParams);
        command.add("-classpath");
        command.add(Paths.get(dataxHomePath, SystemUtils.isOnWindows() ? "lib/*" : "lib/*:.").toString());
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener");
        command.add("-Djava.security.egd=file:///dev/urandom");
        command.add(String.format("-Ddatax.home=%s", dataxHomePath));
        command.add(String.format("-Dlogback.configurationFile=%s", Paths.get(dataxHomePath, "conf/logback.xml")));
        command.add("-Dlog.file.name=datax.all");
        command.add(String.format("-Dlog.dir=%s", logDir.getPath()));
        command.add("com.alibaba.datax.core.Engine");
        command.add("-mode");
        command.add("standalone");
        command.add("-jobid");
        command.add("-1");
        command.add("-job");
        command.add(tmpFilePath);
        return command.toArray(new String[0]);
    }

    private void analysisStatisticsLog(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && !isCanceled()) {
                Matcher matcher = LOG_STATISTICS_PATTERN.matcher(line);
                if (matcher.matches()) {
                    long totalRecords = Long.parseLong(matcher.group(1));
                    bytes = Long.parseLong(matcher.group(2));
                    failed = Long.parseLong(matcher.group(3));
                    object.getTotal().set(totalRecords);
                    object.getCount().set(totalRecords - failed);
                }
                matcher = LOG_DIRTY_RECORD_PATTERN.matcher(line);
                if (matcher.matches()) {
                    LOGGER.warn("Dirty record: {}", line);
                }
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void renameExportFile() throws IOException {
        PluginParameter pluginParameter = jobConfig.getContent()[0].getWriter().getParameter();
        if (!(pluginParameter instanceof TxtWriterPluginParameter)) {
            return;
        }

        File dir = new File(((TxtWriterPluginParameter) pluginParameter).getPath());
        if (!dir.isDirectory() || !dir.exists()) {
            throw new FileNotFoundException(dir.getPath());
        }
        for (File file : dir.listFiles()) {
            Matcher matcher = DATA_FILE_PATTERN.matcher(file.getName());
            if (!file.getName().startsWith(((TxtWriterPluginParameter) pluginParameter).getFileName())
                    || !matcher.matches()) {
                continue;
            }
            String originName = matcher.group(1);
            Path exportPath = file.toPath();
            try {
                exportPath = Files.move(exportPath,
                        Paths.get(((TxtWriterPluginParameter) pluginParameter).getPath(), originName));
            } catch (IOException e) {
                LOGGER.warn("Failed to rename file {} to {}, reason: {}", file.getName(), originName, e.getMessage());
            }
            object.setExportPaths(Collections.singletonList(exportPath.toUri().toURL()));
            return;
        }
        throw new FileNotFoundException(((TxtWriterPluginParameter) pluginParameter).getFileName());
    }
}
