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
package com.oceanbase.odc.service.projectfiles.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.druid.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.projectfiles.exceptions.ChangeFileTooMuchLongException;
import com.oceanbase.odc.service.projectfiles.exceptions.EditVersionConflictException;
import com.oceanbase.odc.service.projectfiles.exceptions.NameDuplicatedException;
import com.oceanbase.odc.service.projectfiles.exceptions.NameTooLongException;
import com.oceanbase.odc.service.projectfiles.utils.ProjectFilePathUtil;

import lombok.Data;

/**
 * 项目文件聚合
 *
 * @author keyangs
 * @date 2024/8/1
 * @since 4.3.2
 */
@Data
public class ProjectFile {
    /**
     * 每次变更数量限制
     */
    private static final int CHANGE_FILE_NUM_LIMIT = 10000;
    /**
     * 同一级目录文件数量限制
     */
    private static final int LEVEL_FILE_NUM_LIMIT = 100;
    private Long id;
    private Date createTime;
    private Date updateTime;
    private Long projectId;
    private Path path;
    private Long version;
    /**
     * 读取时version版本，若此值不为空更新时需要进行version校验
     */
    private Long readVersion;
    private String content;
    private String objectKey;

    private boolean isChanged = false;

    /**
     * 与当前文件同一个父级目录的同级文件，不含当前文件
     */
    private Set<ProjectFile> sameLevelFiles;
    /**
     * 当前文件的所有子文件
     */
    private Set<ProjectFile> subFiles;

    public static ProjectFile create(Long projectId, Path path, String objectKey) {
        return new ProjectFile(null, null, null, projectId, path,
                null, objectKey, null, null);
    }

    public ProjectFile(Long id, Date createTime, Date updateTime, Long projectId, Path path, Long version,
            String objectKey, Set<ProjectFile> sameLevelFiles, Set<ProjectFile> subFiles) {
        this.id = id;
        this.createTime = createTime;
        this.updateTime = updateTime;
        PreConditions.notNull(projectId, "projectId");
        this.projectId = projectId;
        PreConditions.notNull(path, "path");
        this.path = path;
        this.version = version == null ? 0L : version;
        PreConditions.notBlank(objectKey, "objectKey");
        this.objectKey = objectKey;
        this.sameLevelFiles = sameLevelFiles == null ? new HashSet<>() : sameLevelFiles;
        this.subFiles = subFiles == null ? new HashSet<>() : subFiles;

    }

    public List<ProjectFile> getNextLevelFiles() {
        if (CollectionUtils.isEmpty(subFiles)) {
            return new ArrayList<>();
        }
        return subFiles.stream().filter(file -> file.getPath().getLevelNum().equals(this.path.levelNum + 1))
                .sorted((o1, o2) -> Path.getPathSameLevelComparator().compare(o1.getPath(), o2.getPath()))
                .collect(Collectors.toList());
    }

    /**
     * 重命名当前文件及其子文件中
     *
     * @param destination
     * @return 级联更改过的子文件列表，若正常返回当前文件的name肯定是被修改过的
     */
    public Set<ProjectFile> rename(Path destination) {
        if (destination.isExceedNameLengthLimit()) {
            throw new NameTooLongException("name length is over limit " + Path.NAME_LENGTH_LIMIT);
        }
        if (!ProjectFilePathUtil.isRenameValid(this.path, destination)) {
            throw new IllegalArgumentException(
                    "invalid path for rename,from:" + this.path + ",destination:" + destination);
        }
        if (this.isRenameDuplicated(destination)) {
            throw new NameDuplicatedException(
                    "duplicated path for rename,from:" + this.path + ",destination:" + destination);
        }
        if (this.path.rename(this.path, destination)) {
            this.isChanged = true;
        }
        Set<ProjectFile> changedSubFiles = new HashSet<>();
        if (CollectionUtils.isEmpty(subFiles)) {
            return changedSubFiles;
        }
        for (ProjectFile subFile : subFiles) {
            if (subFile.path.rename(this.path, destination)) {
                changedSubFiles.add(subFile);
                subFile.isChanged = true;
            }
            if (changedSubFiles.size() > CHANGE_FILE_NUM_LIMIT) {
                throw new ChangeFileTooMuchLongException("change num is over limit " + CHANGE_FILE_NUM_LIMIT);
            }
        }
        return changedSubFiles;
    }

    /**
     * 编辑当前文件的名称和内容，
     *
     * @param destination 重命名的目标文件path，若与当前文件path相同则不重命名
     * @param objectKey 文件内容编辑之后重新上传到oss的object的objectKey，若与当前的相同则代表未变更内容
     * @param readVersion 前端读取file时version版本，在objectKey变更时，若此值不为空更新时需要进行version校验，若为空则不进行version的检查。
     * @return 变更过的子文件集合
     */
    public Set<ProjectFile> edit(Path destination, String objectKey, Long readVersion) {
        // 若objectKey变更，需要修改内容，且判断readVersion是否符合条件
        if (!StringUtils.equals(this.objectKey, objectKey)) {
            this.objectKey = objectKey;
            this.readVersion = readVersion;
            this.isChanged = true;
            if (isVersionConflict()) {
                throw new EditVersionConflictException("version conflict,current version:" + this.version
                        + ",read version:" + this.readVersion + ",path:" + this.path);
            }
        }

        Set<ProjectFile> changedSubFiles = new HashSet<>();
        // destination和当前path相同，需要修改名称
        if (!this.path.equals(destination)) {
            changedSubFiles = rename(destination);
        }

        return changedSubFiles;
    }

    /**
     * 校验重命名之后的名称是否与已有文件path重复
     *
     * @param destination
     * @return true 重复 ;false 不重复
     */
    public boolean isRenameDuplicated(Path destination) {
        if (CollectionUtils.isEmpty(sameLevelFiles)) {
            return false;
        }
        for (ProjectFile subFile : sameLevelFiles) {
            if (subFile.path.equals(destination)) {
                return true;
            }
        }
        return false;
    }



    /**
     * 判断当前文件更新内容时，是否有版本冲突
     * 
     * @return true 有版本冲突，false 无版本冲突
     */
    private boolean isVersionConflict() {
        return this.readVersion != null && !this.readVersion.equals(this.version);
    }

    /**
     * 是否可以在当前path下创建指定path
     * 
     * @param subFilePath
     * @return
     */
    public boolean canCreateSubFile(Path subFilePath) {
        if (CollectionUtils.isEmpty(this.subFiles)) {
            return true;
        }
        for (ProjectFile subFile : this.subFiles) {
            if (subFile.path.equals(subFilePath)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProjectFile that = (ProjectFile) o;
        return Objects.equals(projectId, that.projectId) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, path);
    }
}
