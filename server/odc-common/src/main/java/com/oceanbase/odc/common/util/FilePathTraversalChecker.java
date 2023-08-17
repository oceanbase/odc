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
package com.oceanbase.odc.common.util;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.Validate;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/2/15 下午5:54
 * @Description: []
 */
@Slf4j
public class FilePathTraversalChecker {
    /**
     * 检查指定路径是否存在文件遍历漏洞
     * 
     * @param filePath the file path to check
     * @param whitePathList file path should be within the whitePath
     * @return false if there exits path traversal; true if there not exits path traversal
     */
    public static boolean checkPathTraversal(String filePath, List<String> whitePathList) {
        Validate.notEmpty(filePath, "filePath");
        Validate.notEmpty(whitePathList, "whitePathList");
        File file = new File(filePath);
        return checkPathTraversal(file, whitePathList);
    }

    /**
     * 检查指定文件是否存在文件遍历漏洞
     * 
     * @param file the file to check
     * @param whitePathList file path should be within the whitePath
     * @return false if there exits path traversal; true if there not exits path traversal
     */
    public static boolean checkPathTraversal(File file, List<String> whitePathList) {
        Validate.notNull(file, "file");
        Validate.notEmpty(whitePathList, "whitePathList");
        try {
            String canonicalPath = file.getCanonicalPath();
            if (StringUtils.isBlank(canonicalPath)) {
                return false;
            }
            for (String whitePath : whitePathList) {
                if (canonicalPath.startsWith(new File(whitePath).getCanonicalPath())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.info("check path traversal failed, fileName={}， reason={}", file.getName(), e.getMessage());
            return false;
        }
    }

}
