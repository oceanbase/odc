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
package com.oceanbase.odc.common.file.zip;

import java.net.URL;
import java.util.List;

/**
 * {@link ZipElement}
 *
 * @author yh263208
 * @date 2022-06-30 20:13
 * @since ODC_release_3.4.0
 */
public interface ZipElement {
    /**
     * get the url of this {@link ZipElement}
     *
     * @return {@link URL}
     */
    URL getUrl();

    /**
     * if this {@link ZipElement} is a directory
     *
     * @return result
     */
    boolean isDirectory();

    /**
     * relative path of a {@link ZipElement}
     *
     * eg. for {@link java.util.zip.ZipEntry}, one implement of the {@link ZipElement} the url of this
     * {@link java.util.zip.ZipEntry} is {@code jar:file:/xxx.zip!/a/b/c.txt} this method returns
     * {@code a/b/c.txt}
     *
     * @return relative path
     */
    String getRelativePath();

    /**
     * name of this {@link ZipElement}
     *
     * eg.
     *
     * for {@link java.util.zip.ZipEntry}, one implement of the {@link ZipElement} the url of this
     * {@link java.util.zip.ZipEntry} is {@code jar:file:/xxx.zip!/a/b/c.txt} this method returns
     * {@code c.txt}
     *
     * @return name value
     */
    String getName();

    /**
     * list all sub {@link ZipElement}
     *
     * @return all sub {@link ZipElement}
     */
    List<ZipElement> listZipElements();

    /**
     * parent {@link ZipElement}, if this {@link ZipElement} is a root one, this method will return
     * {@link null}
     *
     * @return parent {@link ZipElement}
     */
    ZipElement getParent();
}
