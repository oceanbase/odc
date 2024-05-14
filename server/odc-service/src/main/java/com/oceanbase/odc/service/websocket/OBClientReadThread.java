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

import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2020/12/14
 */

@Slf4j
public class OBClientReadThread extends Thread {
    // when client sends a row of data that exceeds a certain length, a ' \r' will be added, causing
    // errors in the ODC front-end display. This charsequence should be filtered.
    private static final Pattern UNEXPECTED_SEQUENCE_PATTERN = Pattern.compile(" \\r");

    private Consumer<String> messageConsumer;
    private InputStream inputStream;
    private volatile Boolean stop = false;

    public OBClientReadThread(InputStream inputStream, Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        log.info("Obclient threadid:{} start to run", this.getId());
        byte[] builder = new byte[1024];
        int total = 0;
        byte[] buf = new byte[1024];
        try {
            int len;
            while (!stop) {
                // to lower time delay for command display
                // refresh interval 16ms, so we can get 60 times result in 1 second, which is suitable for 60 fps in
                // screen
                Thread.sleep(16);
                len = inputStream.read(buf, 0, 1024);
                if (len > 0) {
                    // copy current buffer into builder, use builder for final output
                    int pos = total;
                    total += len;
                    if (total > builder.length) {
                        builder = Arrays.copyOf(builder, total);
                    }
                    System.arraycopy(buf, 0, builder, pos, len);
                    // flush if EOL is not a truncated Chinese character, which will always be converted into \uFFFD
                    // ref: https://www.fileformat.info/info/unicode/char/fffd/index.htm
                    String result = new String(builder);
                    if (!result.endsWith("\uFFFD")) {
                        messageConsumer.accept(UNEXPECTED_SEQUENCE_PATTERN.matcher(result).replaceAll(""));
                        builder = new byte[1024];
                        total = 0;
                    }
                    buf = new byte[1024];
                }
            }
        } catch (InterruptedException e) {
            log.warn("Obclient thread interrupted, threadId={}", this.getId());
            return;
        } catch (Exception e) {
            log.warn("Obclient thread executing failed, threadId={},", this.getId(), e);
        }
        log.info("Obclient thread end, threadId={}", this.getId());
    }

    public void setStopFlag(boolean stop) {
        this.stop = stop;
    }
}
