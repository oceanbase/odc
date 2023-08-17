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

import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ZipElementNode}
 *
 * @author yh263208
 * @date 2022-06-30 17:10
 * @since ODC_release_3.4.0
 */
@Getter
public class ZipElementNode {

    private final ZipElement content;
    private final ZipFileTree zipFileTree;
    private final List<ZipElementNode> childNodes;
    @Setter
    private ZipElementNode parentNode;

    protected ZipElementNode(@NonNull ZipFileTree zipFileTree, @NonNull ZipFile zipFile) {
        this.content = new ZipFileElement(this, zipFile);
        this.zipFileTree = zipFileTree;
        this.childNodes = new LinkedList<>();
    }

    protected ZipElementNode(@NonNull ZipFileTree zipFileTree, @NonNull ZipEntry zipEntry) {
        this.content = new ZipEntryElement(this, zipEntry);
        this.zipFileTree = zipFileTree;
        this.childNodes = new LinkedList<>();
    }

    public void addChildNode(@NonNull ZipElementNode node) {
        this.childNodes.add(node);
    }

    @Override
    public String toString() {
        return this.content.toString();
    }

}
