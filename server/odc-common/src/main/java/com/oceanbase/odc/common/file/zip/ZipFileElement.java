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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import lombok.NonNull;

/**
 * {@link ZipFileElement}
 *
 * @author yh263208
 * @date 2022-06-30 17:11
 * @since ODC_release_3.0.0
 */
public class ZipFileElement implements ZipElement {

    private final ZipFile targetZipFile;
    private final ZipElementNode elementNode;

    public ZipFileElement(@NonNull ZipElementNode elementNode, @NonNull ZipFile targetZipFile) {
        this.targetZipFile = targetZipFile;
        this.elementNode = elementNode;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String getRelativePath() {
        return this.targetZipFile.getName();
    }

    @Override
    public String getName() {
        return this.elementNode.getZipFileTree().getZipFile().getName();
    }

    @Override
    public List<ZipElement> listZipElements() {
        return this.elementNode.getChildNodes().stream().map(ZipElementNode::getContent).collect(Collectors.toList());
    }

    @Override
    public ZipElement getParent() {
        ZipElementNode parentNode = this.elementNode.getParentNode();
        return parentNode == null ? null : parentNode.getContent();
    }

    @Override
    public URL getUrl() {
        File file = this.elementNode.getZipFileTree().getZipFile();
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return this.targetZipFile.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ZipFileElement)) {
            return false;
        }
        ZipFileElement that = (ZipFileElement) obj;
        return Objects.equals(this.getUrl(), that.getUrl());
    }

    @Override
    public int hashCode() {
        return this.getUrl().toString().hashCode();
    }

}
