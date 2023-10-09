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
package com.oceanbase.odc.plugin.task.api.datatransfer.model;

/**
 * the encoding format enum
 *
 * @author yh263208
 * @date 2021-03-24 21:30
 * @since ODC_release_2.4.1
 */
public enum EncodingType {
    /**
     * ascii encoding
     */
    ASCII("ASCII"),
    /**
     * iso8859-1 encoding
     */
    ISO_8859_1("ISO-8859-1"),
    /**
     * gb2312 encoding
     */
    GB2312("GB2312"),
    /**
     * gbk encoding
     */
    GBK("GBK"),
    /**
     * gb18030 encoding
     */
    GB18030("GB18030"),
    /**
     * utf-8 encoding
     */
    UTF_8("UTF-8"),
    /**
     * utf-16 encoding
     */
    UTF_16("UTF-16"),
    /**
     * utf-32 encoding
     */
    UTF_32("UTF-32"),
    /**
     * big5 encoding
     */
    BIG5("BIG5");

    private String alias;

    EncodingType(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return this.alias;
    }
}
