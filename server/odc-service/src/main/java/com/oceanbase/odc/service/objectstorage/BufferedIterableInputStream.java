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
package com.oceanbase.odc.service.objectstorage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/15 下午6:26
 * @Description: []
 */
@Slf4j
public class BufferedIterableInputStream extends BufferedInputStream implements Iterator<byte[]> {

    private int idx;

    private final int totalSize;

    private final int blockSize;

    public BufferedIterableInputStream(InputStream inputStream, int buffSize, long totalSize) {
        super(inputStream, buffSize);
        this.blockSize = buffSize;
        this.totalSize = (int) totalSize;
    }

    @Override
    public boolean hasNext() {
        return getReadSize() < totalSize;
    }

    @Override
    public byte[] next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No block exists.");
        }
        byte[] nextBlock = new byte[getNextBlockSize()];
        try {
            // 读取流信息失败，返回异常信息.
            if (this.read(nextBlock, 0, nextBlock.length) == -1) {
                throw new RuntimeException("Unexpected end of input stream.");
            }
            ++idx;
            return nextBlock;
        } catch (IOException e) {
            log.warn("Read input stream fail.", e);
            throw new RuntimeException(e);
        }
    }

    private int getNextBlockSize() {
        int len = blockSize;
        int readSize = getReadSize();
        if (readSize + blockSize > totalSize) {
            // 获取最后一块的块大小.
            len = totalSize - readSize;
        }
        return len;
    }

    private int getReadSize() {
        return idx * blockSize;
    }

}

