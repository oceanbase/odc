/*
 * Copyright (c) 2024 OceanBase.
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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.service.projectfiles.model.ProjectFileLocation;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileType;
import com.oceanbase.odc.service.projectfiles.utils.ProjectFilePathUtil;

import lombok.Getter;

/**
 * 项目文件路径值对象
 *
 * @author keyang
 * @date 2024/08/01
 * @since 4.3.2
 */
@Getter
public class Path {
    /**
     * 文件名长度限制
     */
    protected static final int NAME_LENGTH_LIMIT = 64;

    List<String> parentPathItems;
    /**
     * path层级数，从1开始,文件夹数量为pathItems.size-1，文件数量为pathItems.size
     */
    Integer levelNum;
    String name;
    ProjectFileType type;
    ProjectFileLocation location;

    public Path(String path) {
        List<String> items = ProjectFilePathUtil.splitPathToItems(path);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        Optional<ProjectFileLocation> locationOptional = ProjectFilePathUtil.getPathLocation(items);
        if (!locationOptional.isPresent()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        Optional<ProjectFileType> pathTypeOptional = ProjectFilePathUtil.getPathType(items);
        if (!pathTypeOptional.isPresent()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        Optional<String> standardPathOptional = ProjectFilePathUtil.convertItemsToPath(items);
        if (!standardPathOptional.isPresent()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        Optional<String> pathNameOptional = ProjectFilePathUtil.getPathName(items);
        if (!pathNameOptional.isPresent()) {
            throw new IllegalArgumentException("invalid path : " + path);
        }
        this.name = pathNameOptional.get();
        this.type = pathTypeOptional.get();
        this.location = locationOptional.get();
        this.levelNum = this.isFile() ? items.size() : items.size() - 1;
        this.parentPathItems = ProjectFilePathUtil.addSeparatorToItemsEnd(items.subList(0, levelNum - 1));
    }

    public String getStandardPath() {
        List<String> pathItems = this.parentPathItems.subList(0, this.parentPathItems.size() - 1);
        pathItems.add(name);
        if (this.isDirectory()) {
            ProjectFilePathUtil.addSeparatorToItemsEnd(pathItems);
        }
        return ProjectFilePathUtil.convertItemsToPath(pathItems)
                // 正常情况下是不会返回这个异常的，这里增加异常处理只是位了保证程序完整性（需要处理Optional为空的情况）
                .orElseThrow(() -> new IllegalArgumentException("Invalid Path Object : " + this));
    }

    /**
     * 获取父path，如果是/Worksheets/、/Repos/git/之类的回返回空
     *
     * @return
     */
    public Optional<Path> getParentPath() {
        boolean canGetParent = false;
        switch (location) {
            case WORKSHEETS:
                canGetParent = levelNum > 1;
                break;
            case REPOS:
                canGetParent = levelNum > 2;
                break;
        }
        if (!canGetParent) {
            return Optional.empty();
        }
        Optional<String> parentStandardPathOptional = ProjectFilePathUtil.convertItemsToPath(this.parentPathItems);
        return parentStandardPathOptional.map(Path::new);

    }

    /**
     * 重命名，只有当前path能够和from匹配才会重命名为destination并返回true，不匹配的则不进行重命名，返回false
     * 这里的from和destination需要满足{@link ProjectFilePathUtil#isRenameValid}，
     * 之所以不在这里增加这个校验的原因是在多次重命名时不会仅从重复校验。
     * 
     * @param from 需要被重命名的path
     * @param destination 重命名后的path
     * @return 是否进行了重命名
     */
    public boolean rename(Path from, Path destination) {
        if (!this.isRenameMatch(from)) {
            return false;
        }
        // 这里是对from本身进行重命名
        if (from.levelNum.equals(this.levelNum)) {
            this.name = destination.name;
            return true;
        }
        // 这里是对from的子集进行重命名
        this.parentPathItems.set(this.levelNum - 2, destination.name);
        return true;
    }

    /**
     * 匹配规则：当前path与from相同或者为from的子集
     * 
     * @param from
     * @return
     */
    public boolean isRenameMatch(Path from) {
        // 当前path和from相同
        if (this.equals(from)) {
            return true;
        }
        // 当前path是from的子集，这个时候from需要为非文件类型（有子集）
        return from.isDirectory() && this.levelNum > from.levelNum
                && CollectionUtils.isEqualCollection(this.parentPathItems.subList(0, this.levelNum - 2),
                        from.parentPathItems.subList(0, from.levelNum - 1))
                && this.parentPathItems.get(this.levelNum - 2).equals(from.name);
    }

    /**
     * 判断当前path是否可以重命名
     * 
     * @return
     */
    public boolean canRename() {
        // 只有/Worksheets/和/Repos/RepoName/的子集才能重命名
        return this.location == ProjectFileLocation.WORKSHEETS && this.levelNum > 1
                || this.location == ProjectFileLocation.REPOS && this.levelNum > 2;
    }

    /**
     * Path排序的comparator，根据type+name排序
     *
     * @return
     */
    public static Comparator<Path> getPathSameLevelComparator() {
        return (o1, o2) -> {
            if (o1.getType() == o2.getType()) {
                return o1.getName().compareTo(o2.getName());
            }
            return -Integer.compare(o1.getType().getOrder(), o2.getType().getOrder());
        };
    }

    public boolean isFile() {
        return this.type == ProjectFileType.FILE;
    }

    public boolean isDirectory() {
        return this.type != ProjectFileType.FILE;
    }

    public boolean isExceedNameLengthLimit() {
        return this.getName().length() > NAME_LENGTH_LIMIT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Path path = (Path) o;
        return CollectionUtils.isEqualCollection(parentPathItems, path.parentPathItems)
                && Objects.equals(name, path.name)
                && type == path.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentPathItems, name, type);
    }

    @Override
    public String toString() {
        return getStandardPath();
    }
}
