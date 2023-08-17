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
package com.oceanbase.odc.service.common;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author wenniu.ly
 * @date 2021/1/26
 */
public class FileRecyclerTest {

    private String basicPath;
    private String userPath;
    private String fileName = "aa.txt";

    @Before
    public void setUp() throws InterruptedException {
        basicPath = this.getClass().getClassLoader().getResource("").getPath() + "sourcefiles/";
        userPath = basicPath + "1/";// 1 here means user id
        File folder = new File(userPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(userPath + fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException("can not create file", e);
            }
        }
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    public void test() throws InterruptedException {
        FileRecycler fileRecycler = new FileRecycler(basicPath, 10L);
        Thread t = new Thread(fileRecycler);
        t.start();
        t.join();
        File file = new File(userPath + fileName);
        Assert.assertTrue(!file.exists());
    }

    @After
    public void clean() {
        File file = new File(userPath + fileName);
        if (file.exists()) {
            file.delete();
        }
    }
}
