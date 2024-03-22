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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.task.executor.logger.LogUtils;

/**
 * @author yaobin
 * @date 2024-03-22
 * @since 4.2.4
 */
public class TestLogUtils {

    @Test
    public void test_readPartLogContent() {
        String fileName = "./test.log";
        File file = new File(fileName);
        try (FileOutputStream a = new FileOutputStream(file)) {
            a.write("test".getBytes());
            a.write("\n".getBytes());
            a.write("java".getBytes());
            a.write("\n".getBytes());
            a.write("go".getBytes());
            a.write("\n".getBytes());
            String s = LogUtils.getLogContent(fileName, 2L, 100L);
            Assert.assertTrue(s.endsWith("java\ngo\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            file.delete();
        }
    }

    @Test
    public void test_readAllLogContent() {
        String fileName = "./test2.log";
        File file = new File(fileName);
        try (FileOutputStream a = new FileOutputStream(file)) {
            a.write("test".getBytes());
            a.write("\n".getBytes());
            a.write("java".getBytes());
            a.write("\n".getBytes());
            a.write("go".getBytes());
            a.write("\n".getBytes());
            String s = LogUtils.getLogContent(fileName, 3L, 100L);
            Assert.assertEquals("test\njava\ngo\n", s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            file.delete();
        }
    }
}
