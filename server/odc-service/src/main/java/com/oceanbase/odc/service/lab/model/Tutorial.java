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
package com.oceanbase.odc.service.lab.model;

import java.util.Date;

import com.oceanbase.odc.metadb.lab.TutorialEntity;

import lombok.Data;

@Data
public class Tutorial {
    /**
     * ID of tutorial
     */
    private Long id;

    /**
     * Record insertion time
     */
    private Date createTime;

    /**
     * Record modification time
     */
    private Date updateTime;

    /**
     * Creator id, references iam_user(id)
     */
    private Long creatorId;

    /**
     * Last modifier id, references iam_user(id)
     */
    private Long lastModifierId;

    /**
     * Tutorial name
     */
    private String name;

    /**
     * Author name
     */
    private String author;

    /**
     * Introduction of content
     */
    private String overview;

    /**
     * Filename of the tutorial
     */
    private String filename;

    /**
     * Operation content
     */
    private String content;

    /**
     * Language
     */
    private String language;

    public Tutorial() {

    }

    public Tutorial(Long id, String name, String overview, String filename, String language) {
        this.id = id;
        this.name = name;
        this.overview = overview;
        this.filename = filename;
        this.language = language;
    }

    public Tutorial(TutorialEntity tutorialEntity) {
        this.id = tutorialEntity.getId();
        this.createTime = tutorialEntity.getCreateTime();
        this.updateTime = tutorialEntity.getUpdateTime();
        this.creatorId = tutorialEntity.getCreatorId();
        this.lastModifierId = tutorialEntity.getLastModifierId();
        this.name = tutorialEntity.getName();
        this.author = tutorialEntity.getAuthor();
        this.overview = tutorialEntity.getOverview();
        this.filename = tutorialEntity.getFilename();
        this.content = tutorialEntity.getContent();
        this.language = tutorialEntity.getAuthor();
    }

}
