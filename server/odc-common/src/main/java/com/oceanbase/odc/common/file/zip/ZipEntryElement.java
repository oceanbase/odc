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

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import lombok.NonNull;

/**
 * {@link ZipEntryElement}
 *
 * @author yh263208
 * @date 2022-06-30 17:12
 * @since ODC_release_3.4.0
 */
public class ZipEntryElement implements ZipElement {

    private final ZipEntry target;
    private final ZipElementNode elementNode;

    protected ZipEntryElement(@NonNull ZipElementNode elementNode, @NonNull ZipEntry target) {
        this.target = target;
        this.elementNode = elementNode;
    }

    @Override
    public boolean isDirectory() {
        return this.target.isDirectory();
    }

    @Override
    public String getRelativePath() {
        return this.target.getName();
    }

    @Override
    public String getName() {
        ZipElementNode parentNode = elementNode.getParentNode();
        if (parentNode == null) {
            throw new IllegalStateException("Can not find parent node");
        }
        String longName = parentNode.getContent().getRelativePath();
        String nodeName = getRelativePath();
        if (target.isDirectory()) {
            int index = nodeName.indexOf(longName);
            if (index == -1) {
                return nodeName.substring(0, nodeName.length() - 1);
            }
            return nodeName.substring(index + longName.length(), nodeName.length() - 1);
        }
        int index = nodeName.indexOf(longName);
        if (index == -1) {
            return nodeName;
        }
        return nodeName.substring(index + longName.length());
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
        StringBuilder builder = new StringBuilder("jar:file:")
                .append(this.elementNode.getZipFileTree().getZipFile().getAbsolutePath())
                .append("!/")
                .append(target.getName());
        try {
            return new URI(null, null, builder.toString(), null).toURL();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return this.target.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ZipEntryElement)) {
            return false;
        }
        ZipEntryElement that = (ZipEntryElement) obj;
        return Objects.equals(this.getUrl(), that.getUrl());
    }

    @Override
    public int hashCode() {
        return this.getUrl().toString().hashCode();
    }

}
