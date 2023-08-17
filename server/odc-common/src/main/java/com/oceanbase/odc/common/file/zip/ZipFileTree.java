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
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ZipFileTree}
 *
 * @author yh263208
 * @date 2022-06-30 12:09
 * @since ODC_release_3.4.0
 */
@Slf4j
public class ZipFileTree {

    @Getter
    private final File zipFile;
    private final ZipElementNode rootNode;

    public ZipFileTree(@NonNull String filePath) throws IOException {
        if (!filePath.endsWith(".zip") && !filePath.endsWith(".jar")) {
            throw new IllegalArgumentException("Invalid file path, " + filePath);
        }
        this.zipFile = new File(filePath);
        if (!zipFile.exists() || !zipFile.isFile()) {
            throw new IllegalArgumentException("Invalid file path, " + filePath);
        }
        try (ZipFile tmpZipFile = new ZipFile(filePath)) {
            this.rootNode = new ZipElementNode(this, tmpZipFile);
            buildTree(tmpZipFile);
        }
    }

    public ZipFileTree(@NonNull File file) throws IOException {
        this(file.getAbsolutePath());
    }

    public void forEach(@NonNull BiConsumer<Integer, ZipElement> consumer) {
        innerDeepFirstForEach(rootNode, (d, e) -> consumer.accept(d, e.getContent()), 0);
    }

    public List<ZipElement> filter(@NonNull Predicate<ZipElement> predicate) {
        return innerFilterEntryNode(e -> predicate.test(e.getContent())).stream().map(
                ZipElementNode::getContent).collect(Collectors.toList());
    }

    public ZipElement getRootElement() {
        return this.rootNode.getContent();
    }

    private void innerDeepFirstForEach(ZipElementNode root,
            BiConsumer<Integer, ZipElementNode> consumer, int depth) {
        try {
            consumer.accept(depth, root);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to consume a node, node={}", root.getContent().getRelativePath(), e);
            }
        }
        for (ZipElementNode item : root.getChildNodes()) {
            innerDeepFirstForEach(item, consumer, depth + 1);
        }
    }

    private List<ZipElementNode> innerFilterEntryNode(@NonNull Predicate<ZipElementNode> predicate) {
        List<ZipElementNode> returnVal = new LinkedList<>();
        innerDeepFirstForEach(rootNode, (depth, entryNode) -> {
            if (predicate.test(entryNode)) {
                returnVal.add(entryNode);
            }
        }, 0);
        return returnVal;
    }

    private void buildTree(@NonNull ZipFile zipFile) {
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        Set<String> left = new HashSet<>();
        Set<String> exists = new HashSet<>();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            String name = zipEntry.getName();
            left.remove(name);
            exists.add(name);
            List<String> fullPath = getFullPath(zipEntry);
            if (!fullPath.isEmpty()) {
                left.addAll(fullPath.stream().filter(s -> !exists.contains(s)).collect(Collectors.toSet()));
            }
            addNode(new ZipElementNode(this, zipEntry));
        }
        for (String name : left) {
            addNode(new ZipElementNode(this, new ZipEntry(name)));
        }
    }

    /**
     * 某些平台上打包出来的zip文件在解析时可能会丢失部分目录结构，例如原始的目录结构为：
     * 
     * <pre>
     *         | a
     *         | _b
     *         | __c.txt
     *         | __d.sql
     * </pre>
     * 
     * 预期的遍历结果是:
     * 
     * <pre>
     *     /a
     *     /a/b
     *     /a/b/c.txt
     *     /a/b/d/sql
     * </pre>
     * 
     * 但某些平台打包的zip解析结果会丢失部分目录，变为：
     * 
     * <pre>
     *     /a
     *     /a/b/c.txt
     *     /a/b/d.sql
     * </pre>
     * 
     * 导入导出获取对象类型时是通过最深层文件的上级目录名称来实现的，丢失中间层目录就会导致识别失败。
     * 解决方法在于使用{@code /}正斜杠切分文件路径，确保文件路径上的每个目录都进入到最终的文件树上。
     */
    private List<String> getFullPath(@NonNull ZipEntry zipEntry) {
        String separtor = "/";
        String name = zipEntry.getName();
        String[] names = name.split(separtor);
        List<String> returnVal = new LinkedList<>();
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < names.length - 1; i++) {
            if (StringUtils.isBlank(names[i])) {
                continue;
            }
            returnVal.add(prefix + names[i] + separtor);
            prefix.append(names[i]).append(separtor);
        }
        return returnVal;
    }

    private void addNode(@NonNull ZipElementNode node) {
        List<ZipElementNode> parentNodes = innerFilterEntryNode(entry -> isParentNode(entry, node));
        if (parentNodes.isEmpty()) {
            throw new IllegalStateException("Can not find parent node");
        }
        ZipElementNode targetParent = null;
        for (ZipElementNode item : parentNodes) {
            if (item.getContent() instanceof ZipFileElement) {
                continue;
            }
            if (targetParent == null) {
                targetParent = item;
                continue;
            }
            if (targetParent.getContent().getRelativePath().length() < item.getContent().getRelativePath().length()) {
                targetParent = item;
            }
        }
        if (targetParent == null) {
            targetParent = rootNode;
        }
        List<ZipElementNode> child = new LinkedList<>();
        targetParent.getChildNodes().removeIf(entryNode -> {
            if (isParentNode(node, entryNode)) {
                child.add(entryNode);
                return true;
            }
            return false;
        });
        node.setParentNode(targetParent);
        targetParent.addChildNode(node);
        for (ZipElementNode item : child) {
            item.setParentNode(node);
            node.addChildNode(item);
        }
    }

    private boolean isParentNode(@NonNull ZipElementNode src, @NonNull ZipElementNode dest) {
        if (src.getContent().getRelativePath().equals(dest.getContent().getRelativePath())) {
            return false;
        }
        if (src.getContent() instanceof ZipFileElement) {
            return true;
        }
        if (dest.getContent() instanceof ZipFileElement) {
            return false;
        }
        return dest.getContent().getRelativePath().startsWith(src.getContent().getRelativePath());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        forEach((depth, node) -> {
            if (depth > 0) {
                builder.append("|");
            }
            for (int i = 0; i < depth; i++) {
                builder.append("_");
            }
            builder.append(node.getName()).append("\n");
        });
        return builder.toString();
    }

}
