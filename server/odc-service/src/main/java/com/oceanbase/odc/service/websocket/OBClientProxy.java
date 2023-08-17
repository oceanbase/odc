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
package com.oceanbase.odc.service.websocket;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2020/12/14
 */

@Slf4j
public class OBClientProxy implements ClientProxy {

    private ThreadPoolExecutor executor;
    private Consumer<String> messageConsumer;
    private String workDirectory;
    private Long lastAccessTime;
    private PtyProcess ptyProcess;
    private OutputStream ptyOutputStream;
    private OBClientReadThread readThread;

    public OBClientProxy(ThreadPoolExecutor executor, Consumer<String> messageConsumer, String workDirectory) {
        this.executor = executor;
        this.messageConsumer = messageConsumer;
        this.workDirectory = workDirectory;
        this.lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public void connect(String[] commands) {
        try {
            // need get sys environment and pass it to subprocess
            // to avoid socket init failure in Windows os
            Map<String, String> envMap = System.getenv();
            // create work dir before start process
            // to avoid exception in winpty
            createIfNotExist();
            // 这里的commands，多项参数必须完全分开，连在一起会导致失败
            ptyProcess = new PtyProcessBuilder().setCommand(commands).setRedirectErrorStream(true)
                    .setEnvironment(envMap).setDirectory(workDirectory).start();
            ptyOutputStream = ptyProcess.getOutputStream();
        } catch (Exception e) {
            log.warn("Failed to connect obclient", e);
            if (Objects.nonNull(ptyProcess)) {
                ptyProcess.destroyForcibly();
                ptyProcess = null;
            }
            throw new RuntimeException("Failed to connect obclient", e);
        }
        readThread = new OBClientReadThread(ptyProcess.getInputStream(), messageConsumer);
        // if active thread count equals maximum thread count, do not block but throw exception directly
        int activeCount = executor.getActiveCount();
        int maximumPoolSize = executor.getMaximumPoolSize();
        if (activeCount >= maximumPoolSize) {
            throw new OverLimitException(LimitMetric.OBCLIENT_INSTANCE_COUNT, (double) maximumPoolSize,
                    String.format("active obclient count %d larger than maximum limit %d",
                            activeCount, maximumPoolSize));
        }
        executor.submit(readThread);
    }

    private void createIfNotExist() {
        File workDir = new File(workDirectory);
        if (!workDir.exists()) {
            boolean success = workDir.mkdirs();
            if (!success) {
                log.error("Create work directory failed, workDirectory={}", workDirectory);
                throw new InternalServerError(ErrorCodes.FileCreateUnauthorized,
                        String.format("Unable to create work directory, workDirectory=%s", workDirectory));
            }
        }
    }

    @Override
    public void write(String command) throws IOException {
        ptyOutputStream.write(command.getBytes());
        ptyOutputStream.flush();
    }

    public void setLastAccessTime(Long accessTime) {
        this.lastAccessTime = accessTime;
    }

    public Long getLastAccessTime() {
        return this.lastAccessTime;
    }

    public Boolean isAlive() {
        return this.ptyProcess.isRunning();
    }

    @Override
    public void close() {
        if (readThread != null) {
            readThread.setStopFlag(true);
            readThread = null;
        }
        if (ptyProcess != null) {
            try {
                // ptyProcess will close its own input&output stream
                ptyProcess.destroy();
                ptyProcess = null;
            } catch (Exception e) {
                log.error("Failed to close subprocess", e);
            }
        }
    }
}
