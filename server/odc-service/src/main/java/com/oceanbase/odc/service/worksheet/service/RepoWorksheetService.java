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
package com.oceanbase.odc.service.worksheet.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.git.GitClientOperator;
import com.oceanbase.odc.service.git.repo.GitRepositoryManager;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * the handle of worksheets in /Repos/
 *
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Service
@Slf4j
public class RepoWorksheetService implements WorksheetService {
    private static final String REPOS_FORMAT = "/Repos/%s/";
    @Autowired
    private ObjectStorageClient objectStorageClient;
    @Autowired
    private GitRepositoryManager repositoryManager;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Override
    public GenerateWorksheetUploadUrlResp generateUploadUrl(Long projectId, Path path) {
        String objectId = WorksheetUtil.getObjectIdOfRepos(path);
        String uploadUrl = objectStorageClient.generateUploadUrl(objectId).toString();
        return GenerateWorksheetUploadUrlResp.builder().uploadUrl(uploadUrl).objectId(objectId).build();
    }

    @Override
    public WorksheetMetaResp createWorksheet(Long projectId, Path createPath, String objectId, Long totalLength) {
        File dest = getAbsoluteFile(createPath);
        if (dest.exists()) {
            throw new BadRequestException("File already exists");
        }
        try {
            if (createPath.getType() == WorksheetType.FILE) {
                objectStorageClient.downloadToFile(objectId, dest);
            } else {
                FileUtils.forceMkdir(dest);
            }
            submitStoreTask(getGitOperator(createPath), getRepoId(createPath), authenticationFacade.currentUserId());
            return WorksheetMetaResp.builder()
                    .type(createPath.getType())
                    .path(createPath.getStandardPath())
                    .projectId(projectId)
                    .build();
        } catch (IOException e) {
            log.warn("Failed to create file", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public WorksheetResp getWorksheetDetails(Long projectId, Path path) {
        File dest = getAbsoluteFile(path);
        if (!dest.exists() || !dest.isFile()) {
            throw new BadRequestException("Invalid path, must be file");
        }
        try {
            return WorksheetResp.builder()
                    .type(path.getType())
                    .projectId(projectId)
                    .content(FileUtils.readFileToString(dest, StandardCharsets.UTF_8))
                    .path(path.getStandardPath())
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<WorksheetMetaResp> listWorksheets(Long projectId, Path path, Integer depth, String nameLike) {
        GitClientOperator gitOperator = getGitOperator(path);
        java.nio.file.Path repo = gitOperator.getRepoDir().toPath();
        File dir = getAbsoluteFile(path);
        if (!dir.isDirectory()) {
            throw new BadRequestException("Invalid path, must be directory");
        }
        String prefix = String.format(REPOS_FORMAT, path.getPathAt(1).getName());
        try (Stream<java.nio.file.Path> paths = Files.find(dir.toPath(), depth,
                (filePath, fileAttr) -> StringUtils.containsIgnoreCase(filePath.getFileName().toString(), nameLike))) {
            return paths
                    .filter(p -> !p.equals(dir.toPath()))
                    .map(p -> {
                        WorksheetType type;
                        if (p.toFile().isDirectory()) {
                            type = WorksheetType.DIRECTORY;
                        } else {
                            type = WorksheetType.FILE;
                        }
                        return WorksheetMetaResp.builder()
                                .type(type)
                                .projectId(projectId)
                                .path(prefix + repo.relativize(p))
                                .build();
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BatchOperateWorksheetsResp batchUploadWorksheets(Long projectId,
            BatchCreateWorksheets batchCreateWorksheets) {
        throw new UnsupportedException();
    }

    @Override
    public BatchOperateWorksheetsResp batchDeleteWorksheets(Long projectId, List<Path> paths) {
        BatchOperateWorksheetsResp resp = new BatchOperateWorksheetsResp();
        if (CollectionUtils.isEmpty(paths)) {
            return null;
        }
        List<WorksheetMetaResp> successfulFiles = new ArrayList<>();
        List<WorksheetMetaResp> failedFiles = new ArrayList<>();
        for (Path path : paths) {
            WorksheetMetaResp meta = WorksheetMetaResp.builder()
                    .type(path.getType())
                    .projectId(projectId)
                    .path(path.getStandardPath())
                    .build();
            File dest = getAbsoluteFile(path);
            if (!dest.exists()) {
                failedFiles.add(meta);
            } else {
                try {
                    FileUtils.forceDelete(dest);
                    successfulFiles.add(meta);
                } catch (IOException e) {
                    log.warn("Failed to delete file {}", dest, e);
                    failedFiles.add(meta);
                    resp.setAllSuccessful(false);
                }
            }
        }
        Path path = paths.get(0);
        submitStoreTask(getGitOperator(path), getRepoId(path), authenticationFacade.currentUserId());
        resp.setSuccessfulFiles(successfulFiles);
        resp.setFailedFiles(failedFiles);
        return resp;
    }

    @Override
    public List<WorksheetMetaResp> renameWorksheet(Long projectId, Path path, Path destinationPath) {
        File localFile = getAbsoluteFile(path);
        File dest = getAbsoluteFile(destinationPath);
        if (!localFile.exists()) {
            throw new BadRequestException("File not exists");
        }
        if (dest.exists()) {
            throw new BadRequestException("File already exists");
        }
        try {
            if (localFile.isDirectory()) {
                FileUtils.moveDirectory(localFile, dest);
            } else {
                FileUtils.moveFile(localFile, dest);
            }
            submitStoreTask(getGitOperator(path), getRepoId(path), authenticationFacade.currentUserId());
            return Collections.singletonList(WorksheetMetaResp.builder()
                    .type(destinationPath.getType())
                    .projectId(projectId)
                    .path(destinationPath.getStandardPath())
                    .build());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to rename file", e);
        }
    }

    @Override
    public List<WorksheetMetaResp> editWorksheet(Long projectId, Path path, String objectId, Long totalLength,
            Long readVersion) {
        File localDest = getAbsoluteFile(path);
        try {
            objectStorageClient.downloadToFile(objectId, localDest);
            submitStoreTask(getGitOperator(path), getRepoId(path), authenticationFacade.currentUserId());
            return Collections.singletonList(WorksheetMetaResp.builder()
                    .type(path.getType())
                    .projectId(projectId)
                    .path(path.getStandardPath())
                    .build());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to download file", e);
        }
    }

    @Override
    public String getDownloadUrl(Long projectId, Path path) {
        throw new UnsupportedException();
    }

    @Override
    public void downloadPathsToDirectory(Long projectId, List<Path> paths, Path commParentPath,
            File destinationDirectory) {
        throw new UnsupportedException();
    }

    private Long getRepoId(Path path) {
        return Long.parseLong(path.getPathAt(1).getName());
    }

    private GitClientOperator getGitOperator(Path path) {
        long repoId = Long.parseLong(path.getPathAt(1).getName());
        return repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
    }

    private File getAbsoluteFile(Path path) {
        GitClientOperator gitOperator = getGitOperator(path);
        File repoDir = gitOperator.getRepoDir();
        String relativePath;
        if (path.isGitRepo()) {
            relativePath = "/";
        } else {
            Path repoPath = path.getPathAt(1);
            relativePath = path.stripPrefix(repoPath).orElseThrow(IllegalArgumentException::new);
        }
        return new File(repoDir, relativePath);
    }

    private void submitStoreTask(GitClientOperator operator, Long repoId, Long userId) {
        operator.getExecutor()
                .execute(() -> {
                    try {
                        repositoryManager.storeRepo(operator, repoId, userId);
                    } catch (GitAPIException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
