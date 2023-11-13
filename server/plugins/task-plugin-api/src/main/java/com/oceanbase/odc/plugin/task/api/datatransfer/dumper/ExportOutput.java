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
package com.oceanbase.odc.plugin.task.api.datatransfer.dumper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.common.file.zip.ZipElement;
import com.oceanbase.odc.common.file.zip.ZipFileTree;
import com.oceanbase.tools.loaddump.common.model.Manifest;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ExportOutput}
 *
 * @author yh263208
 * @date 2022-06-29 19:58
 * @since ODC_release_3.4.0
 */
@Getter
@Slf4j
public class ExportOutput {

    public static final String MANIFEST = "MANIFEST.bin";
    public static final String CHECKPOINT = "CHECKPOINT.bin";
    private final URL target;
    private final BinaryFile<Manifest> manifest;
    private final BinaryFile<List<?>> checkpoints;
    private final List<DumpDBObject> dumpDbObjects;

    public ExportOutput(@NonNull File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found, " + file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            this.dumpDbObjects = getDbObjectFolders(file);
            this.manifest = getManifest(file);
            this.checkpoints = getCheckPoints(file);
            try {
                this.target = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        } else if (file.getName().endsWith(".zip")) {
            ZipFileTree tree = new ZipFileTree(file);
            this.dumpDbObjects = getDbObjectFolders(tree);
            this.manifest = getManifest(tree);
            this.checkpoints = getCheckPoints(tree);
            try {
                this.target = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported file," + file.getName());
        }
    }

    public void toFolder(@NonNull File folder) throws IOException {
        if (folder.isFile()) {
            throw new IllegalArgumentException("Target is a file");
        }
        if (!folder.exists()) {
            FileUtils.forceMkdir(folder);
        }
        String parent = folder.getAbsolutePath() + File.separator;
        if (manifest != null) {
            try (InputStream inputStream = manifest.getUrl().openStream();
                    OutputStream outputStream = new FileOutputStream(parent + MANIFEST)) {
                IOUtils.copy(inputStream, outputStream);
            }
        }
        if (checkpoints != null) {
            try (InputStream inputStream = checkpoints.getUrl().openStream();
                    OutputStream outputStream = new FileOutputStream(parent + CHECKPOINT)) {
                IOUtils.copy(inputStream, outputStream);
            }
        }
        for (DumpDBObject dumpDbObject : dumpDbObjects) {
            String dirName = dumpDbObject.getObjectType().getName();
            FileUtils.forceMkdir(new File(parent + dirName));
            for (AbstractOutputFile outputFile : dumpDbObject.getOutputFiles()) {
                String name = parent + dirName + File.separator + outputFile.getFileName();
                try (InputStream inputStream = outputFile.getUrl().openStream();
                        OutputStream outputStream = new FileOutputStream(name)) {
                    IOUtils.copy(inputStream, outputStream);
                }
            }
        }
    }

    public void toZip(@NonNull File target) throws IOException {
        toZip(target, null);
    }

    public void toZip(@NonNull File target, Predicate<AbstractOutputFile> predicate) throws IOException {
        File parentDir = target.getParentFile();
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
        try (FileOutputStream outputStream = new FileOutputStream(target);
                ArchiveOutputStream out = new ZipArchiveOutputStream(outputStream)) {
            if (manifest != null) {
                out.putArchiveEntry(new ZipArchiveEntry(MANIFEST));
                try (InputStream inputStream = manifest.getUrl().openStream()) {
                    IOUtils.copy(inputStream, out);
                }
                out.closeArchiveEntry();
            }
            if (checkpoints != null) {
                out.putArchiveEntry(new ZipArchiveEntry(CHECKPOINT));
                try (InputStream inputStream = checkpoints.getUrl().openStream()) {
                    IOUtils.copy(inputStream, out);
                }
                out.closeArchiveEntry();
            }
            for (DumpDBObject dumpDbObject : dumpDbObjects) {
                String dirName = dumpDbObject.getObjectType().getName();
                out.putArchiveEntry(new ZipArchiveEntry(dirName + "/"));
                out.closeArchiveEntry();
                for (AbstractOutputFile outputFile : dumpDbObject.getOutputFiles()) {
                    if (predicate != null && !predicate.test(outputFile)) {
                        continue;
                    }
                    URL url = outputFile.getUrl();
                    out.putArchiveEntry(new ZipArchiveEntry(dirName + "/" + outputFile.getFileName()));
                    try (InputStream inputStream = url.openStream()) {
                        IOUtils.copy(inputStream, out);
                    }
                    out.closeArchiveEntry();
                }
            }
            out.flush();
        }
        if (!target.exists()) {
            throw new IllegalStateException("Failed to create a zip file");
        }
    }

    public List<AbstractOutputFile> getAllDumpFiles() {
        List<AbstractOutputFile> outputFiles = new LinkedList<>();
        for (DumpDBObject dumpDbObject : this.dumpDbObjects) {
            outputFiles.addAll(dumpDbObject.getOutputFiles());
        }
        return outputFiles;
    }

    public boolean isLegal() {
        return isContainsData() || isContainsSchema();
    }

    public boolean isContainsSchema() {
        return getAllDumpFiles().stream().anyMatch(f -> f instanceof SchemaFile);
    }

    public boolean isContainsData() {
        return getAllDumpFiles().stream().anyMatch(f -> f instanceof DataFile);
    }

    private List<DumpDBObject> getDbObjectFolders(File root) throws IOException {
        /**
         * 遍历根目录，找到所有符合导入导出格式的资源文件
         */
        List<File> outputFiles = new LinkedList<>();
        Files.walkFileTree(Paths.get(root.toURI()), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                File file = path.toFile();
                if (!file.isHidden() && (SchemaFile.instanceOf(file) || DataFile.instanceOf(file))) {
                    outputFiles.add(file);
                }
                return super.visitFile(path, attrs);
            }
        });
        Set<File> objectFolders = outputFiles.stream().map(File::getParentFile).collect(Collectors.toSet());
        List<DumpDBObject> returnVal = new LinkedList<>();
        for (File file : objectFolders) {
            if (!DumpDBObject.isObjectType(file)) {
                continue;
            }
            returnVal.add(new DumpDBObject(file));
        }
        return returnVal;
    }

    private List<DumpDBObject> getDbObjectFolders(ZipFileTree zipFileTree) throws FileNotFoundException {
        List<ZipElement> outputFiles = zipFileTree.filter(element -> {
            if (element.isDirectory()) {
                return false;
            }
            return SchemaFile.instanceOf(element) || DataFile.instanceOf(element);
        });
        Set<ZipElement> objectFolders = outputFiles.stream().map(ZipElement::getParent).collect(Collectors.toSet());
        List<DumpDBObject> returnVal = new LinkedList<>();
        for (ZipElement elt : objectFolders) {
            if (!DumpDBObject.isObjectType(elt)) {
                continue;
            }
            returnVal.add(new DumpDBObject(elt));
        }
        return returnVal;
    }

    private BinaryFile<Manifest> getManifest(File parentDir) throws IOException {
        File manifest = new File(parentDir.getAbsolutePath() + File.separator + MANIFEST);
        if (!manifest.exists()) {
            return null;
        }
        return BinaryFile.newFile(manifest.toURI().toURL());
    }

    private BinaryFile<Manifest> getManifest(ZipFileTree tree) {
        List<ZipElement> nodes = tree.filter(zipEltNode -> MANIFEST.equals(zipEltNode.getName()));
        if (nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() != 1) {
            throw new IllegalStateException("Illegal manifest.bin");
        }
        return BinaryFile.newFile(nodes.get(0).getUrl());
    }

    private BinaryFile<List<?>> getCheckPoints(File parentDir) throws IOException {
        File checkpoints = new File(parentDir.getAbsolutePath() + File.separator + CHECKPOINT);
        if (!checkpoints.exists()) {
            return null;
        }
        return BinaryFile.newFile(checkpoints.toURI().toURL());
    }

    private BinaryFile<List<?>> getCheckPoints(ZipFileTree tree) {
        List<ZipElement> nodes = tree.filter(zipEltNode -> CHECKPOINT.equals(zipEltNode.getName()));
        if (nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() != 1) {
            throw new IllegalStateException("Illegal manifest.bin");
        }
        return BinaryFile.newFile(nodes.get(0).getUrl());
    }

}
