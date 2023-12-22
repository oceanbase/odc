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
package com.oceanbase.odc.service.common.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.util.ResourceUtils;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.InternalServerError;

public class OdcFileUtil {

    /** 绝对路径 **/
    private static String absolutePath = "";

    /** 静态目录 **/
    private static String staticDir = "static/"; // static/tmp/object/data/upload1.jpg

    /** 文件存放的目录 **/
    private static String fileDir = "tmp/";

    public static String transformInputStream(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        String line;

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    /**
     * 上传单个文件 最后文件存放路径为：static/upload/image/test.jpg 文件访问路径为：http://127.0.0.1:8080/upload/image/test.jpg
     * 该方法返回值为：/upload/image/test.jpg
     * 
     * @param inputStream 文件流
     * @param path 文件路径，如：image/
     * @param filename 文件名，如：test.jpg
     * @return 成功：上传后的文件访问路径，失败返回：null
     */
    public static String upload(InputStream inputStream, String path, String filename) {
        // 第一次会创建文件夹
        createDirIfNotExists();

        String resultPath = staticDir + fileDir + path + filename;

        // 存文件
        File uploadFile = new File(absolutePath, resultPath);
        try {
            FileUtils.copyInputStreamToFile(inputStream, uploadFile);
        } catch (IOException e) {
            throw new InternalServerError(ErrorCodes.FileWriteFailed,
                    String.format("faile to write file content, path=%s, filename=%s", path, filename), e);
        }
        return resultPath;
    }

    /**
     * 创建文件夹路径
     */
    private static void createDirIfNotExists() {
        if (!absolutePath.isEmpty()) {
            return;
        }

        // 获取跟目录
        File file = null;
        try {
            file = new File(ResourceUtils.getURL("classpath:").getPath());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("获取根目录失败，无法创建上传目录！");
        }
        if (!file.exists()) {
            file = new File("");
        }

        absolutePath = file.getAbsolutePath();

        File upload = new File(absolutePath, staticDir + fileDir);
        if (!upload.exists()) {
            upload.mkdirs();
        }
    }

    /**
     * 删除文件
     * 
     * @param path 文件访问的路径upload开始 如： /upload/image/test.jpg
     * @return true 删除成功； false 删除失败
     */
    public static boolean deleteFile(String path, String filename) throws Exception {
        File file = getFile(path, filename);
        if (file.exists()) {
            return file.delete();
        }

        return false;
    }

    /**
     * 获取二进制文件流
     * 
     * @param path
     * @param filename
     * @return
     * @throws Exception
     */
    public static InputStream getStream(String path, String filename) throws Exception {
        return new FileInputStream(getFile(path, filename));
    }

    /**
     * 获取文本内容的reader
     * 
     * @param path
     * @param filename
     * @return
     * @throws Exception
     */
    public static Reader getReader(String path, String filename) throws Exception {
        return new FileReader(getFile(path, filename));
    }

    public static File getFile(String path, String filename) {
        createDirIfNotExists();
        String resultPath = staticDir + fileDir + path + filename;
        // 读取文件
        return new File(absolutePath, resultPath);
    }

    public static File[] getFiles(String path) {
        createDirIfNotExists();

        File file = new File(absolutePath, staticDir + fileDir + path);
        if (file.isDirectory()) {
            return file.listFiles();
        }
        return null;
    }

    public static String getAbsolutePath() {
        createDirIfNotExists();
        return absolutePath;
    }

    public static String getStaticPath() {
        createDirIfNotExists();
        return absolutePath + "/" + staticDir + fileDir;
    }

    public static void setAbsolutePath(String absolutePath) {
        OdcFileUtil.absolutePath = absolutePath;
    }

    /**
     * 一次性读取全部文件数据
     * 
     * @param strFile
     */
    public static String readFile(String strFile) throws IOException {
        InputStream input = null;
        try {
            input = new FileInputStream(strFile);
            int available = input.available();
            byte[] bytes = new byte[available];
            input.read(bytes);
            return new String(bytes);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * 删除文件或目录下所有文件
     * 
     * @param file
     * @return
     */
    public static boolean deleteFiles(File file) {
        if (!file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteFiles(f);
            }
        }
        return file.delete();
    }

    /**
     * 压缩文件夹到指定zip文件
     *
     * @param srcDir 源文件夹
     * @param targetFile 目标知道zip文件
     * @throws IOException IO异常，抛出给调用者处理
     */
    public static void zip(String srcDir, String targetFile) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(targetFile)) {
            zip(srcDir, outputStream);
        }
    }

    /**
     * 压缩文件夹到指定输出流中，可以是本地文件输出流，也可以是web响应下载流
     *
     * @param srcDir 源文件夹
     * @param outputStream 压缩后文件的输出流
     * @throws IOException IO异常，抛出给调用者处理
     */
    public static void zip(String srcDir, OutputStream outputStream) throws IOException {
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                ArchiveOutputStream out = new ZipArchiveOutputStream(bufferedOutputStream)) {
            Path start = Paths.get(srcDir);
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    ArchiveEntry entry = new ZipArchiveEntry(dir.toFile(), start.relativize(dir).toString());
                    out.putArchiveEntry(entry);
                    out.closeArchiveEntry();
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try (InputStream input = new FileInputStream(file.toFile())) {
                        ArchiveEntry entry = new ZipArchiveEntry(file.toFile(), start.relativize(file).toString());
                        out.putArchiveEntry(entry);
                        IOUtils.copy(input, out);
                        out.closeArchiveEntry();
                    }
                    return super.visitFile(file, attrs);
                }
            });
            out.flush();
        }
    }

}
